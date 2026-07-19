package com.example.prac.message.application.query

import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.application.dto.PaginationDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Query Handler = 読み取りユースケース本体。トランザクション境界はここ（readOnly）。
// ページング指定（既定値の適用・offset 計算・総ページ数算出）はこの層の責務。
@Service
@Transactional(readOnly = true)
class ListMessagesQueryHandler(
    private val queryService: IMessageQueryService,
) {
    fun handle(query: ListMessagesQuery): ListMessagesQueryResult {
        val currentPage = (query.currentPage ?: DEFAULT_PAGE).coerceAtLeast(1)
        val perPage = (query.perPage ?: DEFAULT_PER_PAGE).coerceAtLeast(1)
        val offset = (currentPage - 1).toLong() * perPage

        val messages = queryService.listPaged(limit = perPage, offset = offset).map { it.toDto() }
        val totalCount = queryService.count()
        val totalPages = if (totalCount == 0L) 0 else ((totalCount + perPage - 1) / perPage).toInt()

        return ListMessagesQueryResult(
            messages = messages,
            pagination = PaginationDto(totalCount, totalPages, currentPage, perPage),
        )
    }

    // Projection → DTO
    private fun MessageProjection.toDto() =
        MessageDto(
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
