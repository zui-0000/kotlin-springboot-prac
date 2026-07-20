package com.example.prac.message.application.query

import com.example.prac.generated.model.CommonResponseMeta
import com.example.prac.generated.model.MessageListPagination
import com.example.prac.generated.model.MessageListResponse
import com.example.prac.generated.model.MessageListResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

// Query Handler = 読み取りユースケース本体。トランザクション境界はここ（readOnly）。
// ページング指定（既定値の適用・offset 計算・総ページ数算出）はこの層の責務。
// 出力は生成スキーマ型(MessageListResponse=エンベロープ)を直接組んで返す。
@Service
@Transactional(readOnly = true)
class ListMessagesQueryHandler(
    private val queryService: IMessageQueryService,
) {
    fun handle(query: ListMessagesQuery): MessageListResponse {
        val currentPage = (query.currentPage ?: DEFAULT_PAGE).coerceAtLeast(1)
        val perPage = (query.perPage ?: DEFAULT_PER_PAGE).coerceAtLeast(1)
        val offset = (currentPage - 1).toLong() * perPage

        val results = queryService.listPaged(limit = perPage, offset = offset).map { it.toResult() }
        val totalCount = queryService.count()
        val totalPages = if (totalCount == 0L) 0 else ((totalCount + perPage - 1) / perPage).toInt()

        return MessageListResponse(
            result = results,
            meta = CommonResponseMeta(respondedAt = OffsetDateTime.now()),
            pagination =
                MessageListPagination(
                    totalCount = totalCount,
                    totalPages = totalPages,
                    currentPage = currentPage,
                    perPage = perPage,
                ),
        )
    }

    // Projection → 生成レスポンスの Result
    private fun MessageProjection.toResult() =
        MessageListResult(
            id = id,
            userId = userId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PER_PAGE = 10
    }
}
