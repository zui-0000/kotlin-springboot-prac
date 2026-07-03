package com.example.prac

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

// Kotest の StringSpec スタイル。Spring 起動不要の純粋な単体テスト。
class HelloControllerTest :
    StringSpec({
        "hello はあいさつメッセージを返す" {
            val controller = HelloController()
            controller.hello() shouldBe mapOf("message" to "Hello, Kotlin + Spring Boot!")
        }
    })
