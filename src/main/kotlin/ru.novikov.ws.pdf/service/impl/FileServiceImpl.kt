package ru.novikov.ws.pdf.service.impl

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.jboss.logging.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import ru.novikov.ws.pdf.endpoint.PdfCollectWSHandler
import ru.novikov.ws.pdf.exception.DirectoryNotFoundException
import ru.novikov.ws.pdf.exception.NotSupportedFormatException
import ru.novikov.ws.pdf.feign.AnotherServiceFeignClient
import ru.novikov.ws.pdf.dto.PdfMetaDataDto
import ru.novikov.ws.pdf.service.FileService
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO

/**
 * Реализация интерфейса по работе со сканером
 * @author novikov_vi
 */
@Service
class FileServiceImpl(
        private val anotherServiceFeignClient: AnotherServiceFeignClient
) : FileService {

    val logger: Logger = Logger.getLogger(javaClass)

    @Value("\${novikov.tempPath}")
    private var tempDir = ""

    override fun sendToAnotherWebService(file: MultipartFile): PdfMetaDataDto {

        if (!file.originalFilename!!.split(".").last().equals("pdf", true)) {
            throw NotSupportedFormatException("Поддерживаются только файлы pdf формата")
        }

        logger.info("Upload file to another web service...")
        val response = anotherServiceFeignClient.upload(file, "someToken")

        return PdfMetaDataDto(
                id = response.body.id,
                numberOfPages = file.inputStream.use { PDDocument.load(it) }.numberOfPages
        )
    }

    override fun saveTempImage(sessionInfo: PdfCollectWSHandler.SessionInfo, base64: String) {

        fun makeTempDirIfNotExists(): Path {
            val path = Paths.get(tempDir, sessionInfo.tempDirId.toString())

            if (!Files.exists(path)) {
                logger.info("Directory not exists, create with path = ${path.toAbsolutePath()}")
                Files.createDirectory(path)
            }
            return path
        }

        fun createFile(tempDirPath: Path, fileNameId: UUID) {
            logger.info("Create temp file with tempDirPath = ${tempDirPath.toAbsolutePath()} and fileName = $fileNameId")
            Files.write(
                    tempDirPath.resolve("$fileNameId.txt"),
                    base64.toByteArray(StandardCharsets.UTF_8)
            )
        }

        val fileNameId = UUID.randomUUID()
        val tempDirPath = makeTempDirIfNotExists()
        createFile(tempDirPath, fileNameId)
        sessionInfo.fileNameIdList.add(fileNameId)
    }

    override fun prepareTempFileToPdf(sessionInfo: PdfCollectWSHandler.SessionInfo, fileName: String): PdfMetaDataDto {

        logger.info("---Start prepare temp file to pdf format---")
        val path = Paths.get(tempDir, sessionInfo.tempDirId.toString())

        if (Files.exists(path)) {

            logger.info("Form pdf and send to filestore")
            val pdfMeta = formPdfAndSend(sessionInfo, fileName)

            logger.info("Delete temp directory with inner files")
            deleteTempDir(sessionInfo)

            return pdfMeta

        } else throw DirectoryNotFoundException("метод prepareTempFileToPdf директория - $path")
    }

    override fun getPreview(base64String: String?): ByteArray {

        ByteArrayInputStream(Base64.getDecoder().decode(base64String)).use { input ->
            val bufImg = ImageIO.read(input)
            val reduceImg = bufImg.getScaledInstance(64, 90, Image.SCALE_FAST)
            val scaledBi = BufferedImage(64, 90, BufferedImage.TYPE_INT_RGB)
            val graphic = scaledBi.createGraphics()
            graphic.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphic.drawImage(reduceImg, 0, 0, null)
            graphic.dispose()

            return ByteArrayOutputStream().use { baos ->
                ImageIO.write(scaledBi, "jpg", baos)
                baos.toByteArray()
            }
        }
    }

    override fun deleteTempDir(sessionInfo: PdfCollectWSHandler.SessionInfo) {
        val path = Paths.get(tempDir, sessionInfo.tempDirId.toString())

        if (Files.exists(path)) {
            logger.info("Delete temp files")
            Files.walk(path)
                    .map(Path::toFile)
                    .forEach { it.delete() }

            try {
                logger.info("Delete temp directory")
                Files.deleteIfExists(path)
            } catch (ex: DirectoryNotEmptyException) {
                logger.info("Directory $path not empty!")
            }
        }
    }

    override fun deleteTempImage(sessionInfo: PdfCollectWSHandler.SessionInfo, indexArray: Int) {
        val fileNameId = sessionInfo.fileNameIdList[indexArray]
        val path = Paths.get(
                tempDir,
                sessionInfo.tempDirId.toString(),
                "$fileNameId.txt"
        )
        try {
            path.toFile().delete()
        } catch (e: Exception) {
            logger.info("File with path = ${path.toAbsolutePath()} do not deleted")
        }

        sessionInfo.fileNameIdList.remove(fileNameId)
    }

    private fun formPdfAndSend(sessionInfo: PdfCollectWSHandler.SessionInfo, fileName: String): PdfMetaDataDto {
        val path = Paths.get(tempDir, sessionInfo.tempDirId.toString())

        return PDDocument().use { doc ->
            ByteArrayOutputStream().use { baos ->

                logger.info("Fill pdf page")
                sessionInfo.fileNameIdList.map {
                    val imageStr = String(
                            Files.readAllBytes(path.resolve("$it.txt")),
                            StandardCharsets.UTF_8
                    )
                    fillPagePdf(doc, imageStr)
                }

                doc.save(baos)
                val multiPart = getMultipart(baos.toByteArray(), fileName)
                baos.close()
                doc.close()
                sendToAnotherWebService(multiPart)
            }
        }
    }

    private fun fillPagePdf(doc: PDDocument, base64: String) {

        ByteArrayInputStream(Base64.getDecoder().decode(base64)).use { input ->
            val bufImg = ImageIO.read(input)

            val pdImage = LosslessFactory.createFromImage(doc, bufImg)
            val width = bufImg.width.toFloat()
            val height = bufImg.height.toFloat()

            val page = PDPage(PDRectangle(width, height))
            doc.addPage(page)

            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { contentStream ->
                contentStream.drawImage(pdImage, 0.0f, 0.0f, width, height)
            }
        }
    }

    private fun getMultipart(byteArray: ByteArray, fileName: String): MockMultipartFile =
            MockMultipartFile(
                    "file",
                    fileName,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    byteArray
            )
}