package com.example.prac.message.presentation

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.CreateMessageRequest
import com.example.prac.message.application.command.CreateMessageCommand
import com.example.prac.message.application.command.CreateMessageCommandHandler
import com.example.prac.message.application.query.ListMessagesQuery
import com.example.prac.message.application.query.ListMessagesQueryHandler
import com.example.prac.message.application.query.MessageView
import com.example.prac.message.domain.Message
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import com.example.prac.generated.model.Message as MessageDto

// プレゼン層。OpenAPI(schema/openapi.yaml)から生成された MessagesApi を実装する（契約優先）。
// 書き込みは Command Handler、読み取りは Query Handler へ振り分ける（CQRS のディスパッチ）。
// Controller の責務は「生成 DTO ⇔ Command/Query・View/集約」の変換とハンドラ呼び出しだけ。
@RestController
class MessageController(
    private val createMessageCommandHandler: CreateMessageCommandHandler,
    private val listMessagesQueryHandler: ListMessagesQueryHandler,
) : MessagesApi {
    // 読み取り経路: Query → Handler → View → 生成 DTO
    override fun listMessages(): ResponseEntity<List<MessageDto>> =
        ResponseEntity.ok(listMessagesQueryHandler.handle(ListMessagesQuery()).map { it.toDto() })

    // 書き込み経路: 生成リクエスト → Command → Handler → 集約 → 生成 DTO
    override fun createMessage(createMessageRequest: CreateMessageRequest): ResponseEntity<MessageDto> =
        ResponseEntity.ok(
            createMessageCommandHandler.handle(CreateMessageCommand(createMessageRequest.content)).toDto(),
        )

    // 読み取りモデル(View) → 生成 DTO
    private fun MessageView.toDto() = MessageDto(id = id, content = content, createdAt = createdAt)

    // 書き込み結果のドメイン集約 → 生成 DTO（VO から素の値を取り出す）
    private fun Message.toDto() = MessageDto(id = id.value, content = content.value, createdAt = createdAt)
}
