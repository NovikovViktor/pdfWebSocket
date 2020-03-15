package ru.novikov.ws.pdf.endpoint

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jboss.logging.Logger
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import ru.novikov.ws.pdf.dto.PdfMetaDataDto
import ru.novikov.ws.pdf.dto.PreviewBase64
import ru.novikov.ws.pdf.dto.StatusSuccessDeleteOkDto
import ru.novikov.ws.pdf.service.FileService
import java.util.*
import kotlin.collections.HashMap

/**
 * Компонент хэндлер для реализации работы с вебсокетом
 * ожидающим поступающие картинки для дальнейшей компановки их в pdf
 * @author novikov_vi
 */
@Component
@CrossOrigin
class PdfCollectWSHandler(
        private val fileService: FileService
) : TextWebSocketHandler() {

    val logger: Logger = Logger.getLogger(javaClass)

    /**
     * Класс для регистрации пользователя сессии
     * @author novikov_vi
     */
    data class SessionInfo(val tempDirId: UUID, val fileNameIdList: MutableList<UUID>)

    /**
     * Класс-перечисление описывающий действия работы с вебсокетом
     * @author novikov_vi
     */
    enum class ActionType(val act: String){ JOIN("join"), APPEND("append"), DELETE ("delete"), FORM_PDF("form")}

    /**
     * Метод для уничтожении сессии при дисконекте
     * @param session отключающаяся сессия
     * @param status статус закрытия
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val sessionInfo = sessionList[session]
                ?: throw Exception("Ошибка при работе с сессией, создайте новую")

        fileService.deleteTempDir(sessionInfo)
        sessionList -= session
    }

    /**
     * Метод для поддержки hand shake'a
     * @param session подключаемая сессия
     * @param message сообщение при подключении
     */
    public override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            jacksonObjectMapper().readTree(message.payload).apply {
                when (this.get("someType").asText()) {

                    ActionType.JOIN.act -> {
                        logger.info("WebSocket: type -> join. Hello!")
                        session.textMessageSizeLimit = 70920 * 1024
                        sessionList[session] = SessionInfo(UUID.randomUUID(), mutableListOf())
                    }

                    ActionType.APPEND.act -> {
                        logger.info("WebSocket: type -> append. Start append temp file in system!")
                        val sessionInfo = sessionList[session]
                                ?: throw Exception("Ошибка при работе с сессией, создайте новую")

                        val base64String = this.get("data").asText()
                        val previewBase64 = fileService.getPreview(base64String)
                        emitPreview(session, PreviewBase64(previewBase64))

                        fileService.saveTempImage(sessionInfo, base64String)
                    }

                    ActionType.DELETE.act -> {
                        logger.info("WebSocket: type -> delete. Start delete temp file in system!")
                        val sessionInfo = sessionList[session]
                                ?: throw Exception("Ошибка при работе с сессией, создайте новую")

                        val indexArray = this.get("data").asInt()
                        fileService.deleteTempImage(sessionInfo, indexArray)
                        emitSuccessDelete(session, StatusSuccessDeleteOkDto(index = indexArray))
                    }

                    ActionType.FORM_PDF.act -> {
                        logger.info("WebSocket: type -> pdfPlease. Start form pdf and close session!")
                        val sessionInfo = sessionList[session]
                                ?: throw Exception("Ошибка при работе с сессией, создайте новую")

                        val fileName = this.get("data").asText()
                        val metaInfo = fileService.prepareTempFileToPdf(sessionInfo, fileName)
                        emitResult(session, metaInfo)
                        session.close()
                    }

                    else -> throw Exception("Ошибка при инициализации сессии веб сокета")
                }
            }
        } catch (e: Exception) {
            throw Exception("Произошла ошибка при инициализации сессии с сообшением: ${e.message}")
        }
    }

    /**
     * Метод для реализации отправки по номеру сессии на сторону клиента
     * @param session сессия для отправки
     * @param metaInf мета-информация о работе создания pdf документа
     */
    private fun emitResult(session: WebSocketSession, metaInf: PdfMetaDataDto) =
            session.sendMessage(TextMessage(jacksonObjectMapper().writeValueAsString(metaInf)))

    /**
     * Метод для реализации отправки по номеру сессии превью картинки на сторону клиента
     * @param session сессия для отправки
     * @param dto превью кратинки для фронта
     */
    private fun emitPreview(session: WebSocketSession, dto: PreviewBase64) =
            session.sendMessage(TextMessage(jacksonObjectMapper().writeValueAsString(dto)))

    /**
     * Сообщение об успешном удалении из массива переданного индекса
     * @param session WebSocketSession - сессия для отправки
     * @param dto StatusSuccessDeleteOkDto - положительный ответ фронту
     */
    private fun emitSuccessDelete(session: WebSocketSession, dto: StatusSuccessDeleteOkDto) =
            session.sendMessage(TextMessage(jacksonObjectMapper().writeValueAsString(dto)))

    /**
     * Компаньон объект для хранение разделямой сессии
     * @author novikov_vi
     */
    companion object {
        /** Хранилище сессий открытых в сокете **/
        val sessionList = HashMap<WebSocketSession, SessionInfo>()
    }
}