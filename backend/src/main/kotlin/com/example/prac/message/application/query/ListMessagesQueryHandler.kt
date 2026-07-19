package com.example.prac.message.application.query

import com.example.prac.message.application.dto.MessageDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Query Handler = 読み取りユースケース本体。トランザクション境界はここ（readOnly）。
// QueryService から Projection を受け取り、DTO に詰め替えて Result に包んで返す。
@Service
@Transactional(readOnly = true)
class ListMessagesQueryHandler(
    private val queryService: IMessageQueryService,
) {
    fun handle(query: ListMessagesQuery): ListMessagesQueryResult = ListMessagesQueryResult(queryService.listAll().map { it.toDto() })

    // Projection → DTO
    private fun MessageProjection.toDto() =
        MessageDto(
            id = id,
            userId = userId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
