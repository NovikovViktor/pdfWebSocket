package ru.novikov.ws.pdf.config

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.stereotype.Component

/**
 * Конфигурация feign
 * @author novikov_vi
 */
@Component
@EnableFeignClients("ru.novikov.ws.pdf.feign")
class FeignConfig