package com.example.prac.message.presentation

import com.example.prac.generated.api.MessagesApi
import com.example.prac.generated.model.CommonResponseMeta
import com.example.prac.generated.model.MessageCreateRequest
import com.example.prac.generated.model.MessageCreateResponse
import com.example.prac.generated.model.MessageCreateResult
import com.example.prac.generated.model.MessageListPagination
import com.example.prac.generated.model.MessageListResponse
import com.example.prac.generated.model.MessageListResult
import com.example.prac.message.application.command.CreateMessageCommand
import com.example.prac.message.application.command.CreateMessageCommandHandler
import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.application.query.ListMessagesQuery
import com.example.prac.message.application.query.ListMessagesQueryHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * メッセージ機能のプレゼン層。生成された [MessagesApi] を実装する（契約優先）。
 *
 * **ルーティングの在り処**: 各エンドポイントの URL・HTTP メソッド(`@RequestMapping`)は生成された
 * [MessagesApi] 側が持つ（正は `schema/openapi.yaml`）。実際の経路は [MessagesApi] を辿れば分かる。
 *
 * **レスポンスはエンベロープ**: `result`(データ本体) + `meta`(respondedAt) [+ `pagination`]。
 * Handler は DTO を返し、ここで生成レスポンス型へ包む。`meta` はレスポンス生成時の関心事なので
 * この層で付与する。get/update/delete は未実装（生成 interface の NOT_IMPLEMENTED 既定のまま）。
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
    ): ResponseEntity<MessageListResponse> {
        val result = listMessagesQueryHandler.handle(ListMessagesQuery(currentPage, perPage))
        return ResponseEntity.ok(
            MessageListResponse(
                result = result.messages.map { it.toListResult() },
                meta = currentMeta(),
                pagination =
                    MessageListPagination(
                        totalCount = result.pagination.totalCount,
                        totalPages = result.pagination.totalPages,
                        currentPage = result.pagination.currentPage,
                        perPage = result.pagination.perPage,
                    ),
            ),
        )
    }

    /** `POST /messages` — メッセージを登録し 201 でエンベロープを返す。userId は認証実装までの暫定入力。 */
    override fun createMessage(messageCreateRequest: MessageCreateRequest): ResponseEntity<MessageCreateResponse> {
        val result =
            createMessageCommandHandler.handle(
                CreateMessageCommand(
                    userId = messageCreateRequest.userId,
                    content = messageCreateRequest.content,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            MessageCreateResponse(result = result.message.toCreateResult(), meta = currentMeta()),
        )
    }

    // 共通メタ（レスポンスを返した日時）。
    private fun currentMeta() = CommonResponseMeta(respondedAt = OffsetDateTime.now())

    private fun MessageDto.toListResult() = MessageListResult(id, userId, content, createdAt, updatedAt)

    private fun MessageDto.toCreateResult() = MessageCreateResult(id, userId, content, createdAt, updatedAt)
}
