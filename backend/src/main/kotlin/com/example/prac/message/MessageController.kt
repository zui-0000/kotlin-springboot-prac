package com.example.prac.message

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.CreateMessageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import com.example.prac.generated.model.Message as MessageDto

// OpenAPI(schema/openapi.yaml)から生成された MessagesApi を実装する（契約優先）。
// ルーティングや入出力の型は生成された interface が持つ。
// Controller はサービス呼び出しと、ドメイン ⇔ 生成 DTO の変換だけを担う。
@RestController
class MessageController(
    private val service: MessageService,
) : MessagesApi {
    override fun listMessages(): ResponseEntity<List<MessageDto>> = ResponseEntity.ok(service.list().map { it.toDto() })

    override fun createMessage(createMessageRequest: CreateMessageRequest): ResponseEntity<MessageDto> =
        ResponseEntity.ok(service.create(createMessageRequest.content).toDto())

    // ドメインの Message → 生成された DTO
    private fun Message.toDto() = MessageDto(id = id, content = content, createdAt = createdAt)
}
