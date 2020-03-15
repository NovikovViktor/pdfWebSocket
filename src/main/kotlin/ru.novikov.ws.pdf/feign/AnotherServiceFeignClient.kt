package ru.novikov.ws.pdf.feign

import feign.Headers
import feign.Param
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import ru.novikov.ws.pdf.dto.AnotherServiceResponseDto

/**
 * Рест запросы в другой веб-сервис
 * @author novikov_vi
 */
@Component
@FeignClient(name = "service", url = "someUrl.ru")
interface AnotherServiceFeignClient {

    @Headers("Content-Type: ${MediaType.MULTIPART_FORM_DATA_VALUE}", "Transfer-Encoding: Chuncked")
    @PostMapping(value = ["/v1/someUpload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@Param("file") file: MultipartFile,
                          @RequestParam(value = "token") token: String): ResponseEntity<AnotherServiceResponseDto>
}