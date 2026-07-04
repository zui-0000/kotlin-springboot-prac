package com.example.prac

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {
    @GetMapping("/hello")
    fun hello(): Map<String, String> = mapOf("message" to "Hello, Kotlin + Spring Boot!")
}
