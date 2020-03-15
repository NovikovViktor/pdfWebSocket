package ru.novikov.ws.pdf.service

import org.springframework.web.multipart.MultipartFile
import ru.novikov.ws.pdf.endpoint.PdfCollectWSHandler
import ru.novikov.ws.pdf.dto.PdfMetaDataDto

/**
 * Сервис для работы с отсканнированными файлами
 * @author novikov_vi
 */
interface FileService {

    /**
     * Метод для отправки файла в fileStore
     * @param file MultipartFile - отсканированный файл
     * @return Int - количество страниц pdf
     */
    fun sendToAnotherWebService (file: MultipartFile): PdfMetaDataDto

    /**
     * Сохранить временный файл в текстовом base64
     * @param sessionInfo SessionInfo - информация по сессии
     * @param base64 String - строка массива байт
     */
    fun saveTempImage(sessionInfo: PdfCollectWSHandler.SessionInfo, base64: String)

    /**
     * Удалить временный файл по индексу массива
     * @param sessionInfo SessionInfo - информация по сессии
     * @param indexArray Int - индекс массива
     */
    fun deleteTempImage(sessionInfo: PdfCollectWSHandler.SessionInfo, indexArray: Int)

    /**
     * Подготовить временные файлы для сборки pdf (вебсокет)
     * @param sessionInfo SessionInfo - информация по сессии
     * @param fileName String - имя файла
     * @return PdfMetaDataDto - мета данные pdf
     */
    fun prepareTempFileToPdf(sessionInfo: PdfCollectWSHandler.SessionInfo, fileName: String): PdfMetaDataDto

    /**
     * Удалить временную папку для хранения файлов
     * @param sessionInfo SessionInfo - информация по сессии
     */
    fun deleteTempDir(sessionInfo: PdfCollectWSHandler.SessionInfo)

    /**
     * Получение превью картинки из отсканенного файла
     * @param base64String String? - строка
     * @return ByteArray - новый массив байт
     */
    fun getPreview(base64String: String?): ByteArray
}