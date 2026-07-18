package com.example.prac.message.application.query

import com.example.prac.message.application.dto.MessageDto
import org.springframework.stereotype.Service

// Query Handler: 純粋な読み取りCRUD。認可は持たない（UseCase 層の責務）。
// QueryService から Projection を受け取り、DTO に詰め替えて Result に包んで返す。
// トランザクション境界は呼び出し元の UseCase(@Transactional(readOnly)) にある。
@Service
class ListMessagesQueryHandler(
    private val queryService: IMessageQueryService,
) {
    fun handle(query: ListMessagesQuery): ListMessagesQueryResult = ListMessagesQueryResult(queryService.listAll().map { it.toDto() })

    // Projection → DTO
    private fun MessageProjection.toDto() =
        MessageDto(
            id = id,
            content = content,
            createdAt = createdAt,
        )
}
