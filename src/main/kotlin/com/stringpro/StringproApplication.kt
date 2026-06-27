package com.stringpro

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StringproApplication

fun main(args: Array<String>) {
    runApplication<StringproApplication>(*args)
}
