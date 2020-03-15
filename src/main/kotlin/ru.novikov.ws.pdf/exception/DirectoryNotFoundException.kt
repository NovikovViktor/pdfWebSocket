package ru.novikov.ws.pdf.exception

import java.lang.RuntimeException

/**
 * Ошибка при которой происходит обращение в несуществующую директорию
 * @author novikov_vi
 */
class DirectoryNotFoundException(msg: String) : RuntimeException(msg)