package com.example.prac.message.presentation

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.CreateMessageRequest
import com.example.prac.message.application.command.CreateMessageCommand
import com.example.prac.message.application.command.CreateMessageCommandHandler
import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.application.query.ListMessagesQuery
import com.example.prac.message.application.query.ListMessagesQueryHandler
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
 * 責務は「生成 Request → Command/Query 変換」「Handler(=ユースケース) 呼び出し」
 * 「結果 DTO → 生成レスポンス変換」のみ。認可・ビジネスフローは Handler に委ねる。
 */
@RestController
class MessageController(
    private val createMessageCommandHandler: CreateMessageCommandHandler,
    private val listMessagesQueryHandler: ListMessagesQueryHandler,
) : MessagesApi {
    /** `GET /messages` — メッセージ一覧を返す（[MessagesApi.listMessages] の実装）。 */
    override fun listMessages(): ResponseEntity<List<MessageResponse>> =
        ResponseEntity.ok(listMessagesQueryHandler.handle(ListMessagesQuery()).messages.map { it.toResponse() })

    /**
     * `POST /messages` — メッセージを登録する（[MessagesApi.createMessage] の実装）。
     * userId は当面リクエスト由来の暫定入力（認証実装時に auth コンテキストへ差し替える）。
     */
    override fun createMessage(createMessageRequest: CreateMessageRequest): ResponseEntity<MessageResponse> =
        ResponseEntity.ok(
            createMessageCommandHandler
                .handle(
                    CreateMessageCommand(
                        userId = createMessageRequest.userId,
                        content = createMessageRequest.content,
                    ),
                ).message
                .toResponse(),
        )

    /** アプリの [MessageDto] → 生成レスポンス(Message) へ変換する。 */
    private fun MessageDto.toResponse() =
        MessageResponse(
            id = id,
            userId = userId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
