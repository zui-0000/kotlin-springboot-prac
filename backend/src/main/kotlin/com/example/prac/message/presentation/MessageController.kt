package com.example.prac.message.presentation

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.CreateMessageRequest
import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.usecase.CreateMessageUseCase
import com.example.prac.message.usecase.ListMessagesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import com.example.prac.generated.model.Message as MessageResponse

/**
 * メッセージ機能のプレゼン層。生成された [MessagesApi] を実装する（契約優先）。
 *
 * **ルーティングの在り処**: 各エンドポイントの URL・HTTP メソッド(`@RequestMapping`)は、この
 * Controller ではなく生成された [MessagesApi] 側が持つ（正は `schema/openapi.yaml`）。override した
 * メソッドが、対応する operationId のエンドポイントに紐づく。実際の経路は [MessagesApi] を辿れば分かる
 * （IDE の "Go to Super Method"、または下記各メソッドの KDoc リンク）。
 *
 * 責務は UseCase の呼び出しと「DTO ⇔ 生成レスポンス」の変換のみ。Request → Command/Query の変換や
 * 認可・ビジネスフローは UseCase 層に委ねる。
 */
@RestController
class MessageController(
    private val createMessageUseCase: CreateMessageUseCase,
    private val listMessagesUseCase: ListMessagesUseCase,
) : MessagesApi {
    /** `GET /messages` — メッセージ一覧を返す（[MessagesApi.listMessages] の実装）。 */
    override fun listMessages(): ResponseEntity<List<MessageResponse>> =
        ResponseEntity.ok(listMessagesUseCase.execute().messages.map { it.toResponse() })

    /** `POST /messages` — メッセージを登録する（[MessagesApi.createMessage] の実装）。 */
    override fun createMessage(createMessageRequest: CreateMessageRequest): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(createMessageUseCase.execute(createMessageRequest).message.toResponse())

    /** アプリの [MessageDto] → 生成レスポンス(Message) へ変換する。 */
    private fun MessageDto.toResponse() = MessageResponse(id = id, content = content, createdAt = createdAt)
}
