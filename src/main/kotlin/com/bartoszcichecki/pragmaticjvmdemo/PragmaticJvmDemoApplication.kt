package com.bartoszcichecki.pragmaticjvmdemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PragmaticJvmDemoApplication

fun main(args: Array<String>) {
    runApplication<PragmaticJvmDemoApplication>(*args)
}
