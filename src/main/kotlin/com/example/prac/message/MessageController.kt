package com.example.prac.message

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// リクエストボディ用（登録時に content だけ受け取る）
data class CreateMessageRequest(
    val content: String,
)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val repository: MessageRepository,
) {
    // 一覧取得
    @GetMapping
    fun list(): List<Message> = repository.findAll()

    // 登録
    @PostMapping
    fun create(
        @RequestBody request: CreateMessageRequest,
    ): Message = repository.save(Message(content = request.content))
}
