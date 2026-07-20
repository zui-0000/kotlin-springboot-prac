package com.example.prac.message.presentation

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.MessageCreateRequest
import com.example.prac.generated.model.MessageCreateResponse
import com.example.prac.generated.model.MessageListResponse
import com.example.prac.message.application.command.CreateMessageCommand
import com.example.prac.message.application.command.CreateMessageCommandHandler
import com.example.prac.message.application.query.ListMessagesQuery
import com.example.prac.message.application.query.ListMessagesQueryHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * メッセージ機能のプレゼン層。生成された [MessagesApi] を実装する（契約優先）。
 *
 * **ルーティングの在り処**: 各エンドポイントの URL・HTTP メソッド(`@RequestMapping`)は生成された
 * [MessagesApi] 側が持つ（正は `schema/openapi.yaml`）。実際の経路は [MessagesApi] を辿れば分かる。
 *
 * **Handler が生成レスポンス型(エンベロープ)を直接返す**方針のため、この Controller は薄い:
 * 「生成 Request → Command/Query 変換」と「HTTP ステータス付与」だけを担う。
 * `result` / `meta` / `pagination` の組み立ては Handler 側にある。
 * get/update/delete は未実装（生成 interface の NOT_IMPLEMENTED 既定のまま）。
 */
@RestController
class MessageController(
    private val createMessageCommandHandler: CreateMessageCommandHandler,
    private val listMessagesQueryHandler: ListMessagesQueryHandler,
) : MessagesApi {
    /** `GET /messages` — メッセージ一覧（エンベロープ + ページネーション）を返す。 */
    override fun listMessages(
        currentPage: Int?,
        perPage: Int?,
    ): ResponseEntity<MessageListResponse> = ResponseEntity.ok(listMessagesQueryHandler.handle(ListMessagesQuery(currentPage, perPage)))

    /** `POST /messages` — メッセージを登録し 201 でエンベロープを返す。userId は認証実装までの暫定入力。 */
    override fun createMessage(messageCreateRequest: MessageCreateRequest): ResponseEntity<MessageCreateResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            createMessageCommandHandler.handle(
                CreateMessageCommand(
                    userId = messageCreateRequest.userId,
                    content = messageCreateRequest.content,
                ),
            ),
        )
}
