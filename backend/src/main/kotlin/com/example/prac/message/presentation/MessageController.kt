package com.example.prac.message.presentation

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.CreateMessageRequest
import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.usecase.CreateMessageUseCase
import com.example.prac.message.usecase.ListMessagesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import com.example.prac.generated.model.Message as MessageResponse

// プレゼン層。OpenAPI(schema/openapi.yaml)から生成された MessagesApi を実装する（契約優先）。
// 責務は「UseCase の呼び出し」と「結果の DTO → 生成レスポンスへの変換」のみ。
// Request → Command/Query の変換や認可・ビジネスフローは UseCase 層に委ねる。
@RestController
class MessageController(
    private val createMessageUseCase: CreateMessageUseCase,
    private val listMessagesUseCase: ListMessagesUseCase,
) : MessagesApi {
    // 読み取り: UseCase → QueryResult(DTOのリスト) → 生成レスポンスへ変換
    override fun listMessages(): ResponseEntity<List<MessageResponse>> =
        ResponseEntity.ok(listMessagesUseCase.execute().messages.map { it.toResponse() })

    // 書き込み: 生成リクエストを UseCase へ渡す（Request→Command 変換は UseCase の責務）→ 生成レスポンスへ変換
    override fun createMessage(createMessageRequest: CreateMessageRequest): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(createMessageUseCase.execute(createMessageRequest).message.toResponse())

    // アプリの DTO → 生成レスポンス(Message)
    private fun MessageDto.toResponse() = MessageResponse(id = id, content = content, createdAt = createdAt)
}
