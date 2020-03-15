package ru.novikov.ws.pdf.exception

import java.lang.RuntimeException

/**
 * Объект исключения при попытке сохранить в другой сервис невалидный формат
 * @author novikov_vi
 */
class NotSupportedFormatException (msg: String) : RuntimeException(msg)