package ru.novikov.ws.pdf.dto

import java.util.*

/**
 * Ответ сервиса
 * @author novikov_vi
 */
data class AnotherServiceResponseDto(
        val id: UUID? = null
)

/**
 * @author novikov_vi
 */
data class PdfMetaDataDto(
        val id: UUID? = null,
        val numberOfPages: Int? = null
)


data class PreviewBase64(
        val previewBase64: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreviewBase64

        if (!previewBase64.contentEquals(other.previewBase64)) return false

        return true
    }

    override fun hashCode(): Int {
        return previewBase64.contentHashCode()
    }
}