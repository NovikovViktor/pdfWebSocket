package ru.novikov.ws.pdf

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * @author novikov_vi
 */
@SpringBootApplication
open class WsPdfApplication

fun main(args: Array<String>) {
    runApplication<WsPdfApplication>(*args)
}