package ru.novikov.ws.pdf.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.*
import ru.novikov.ws.pdf.endpoint.PdfCollectWSHandler

/**
 * Конфигурирование веб-сокета
 * @author novikov_vi
 */
@Configuration
@EnableWebSocket
open class WebSocketConfig(
        private val pdfCollectWSHandler: PdfCollectWSHandler
) : WebSocketConfigurer {

    /**
     * Создание эндпоинта для работы с вебсокетом (SockJS)
     * @param registry обработтчик событий вебсокета
     */
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(pdfCollectWSHandler, "/v1/ws/pdfWork").setAllowedOrigins("*").withSockJS()
    }
}