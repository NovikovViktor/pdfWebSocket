package ru.novikov.ws.pdf.dto

/**
 * Dto для положительного ответа фронту
 * @author novikov_vi
 */
data class StatusSuccessDeleteOkDto(val status: String = "deleteIndexThumbnail", val index: Int)