package com.example.prac.message.infrastructure

import com.example.prac.message.domain.IMessageRepository
import com.example.prac.message.domain.Message
import com.example.prac.message.domain.MessageContent
import com.example.prac.message.domain.MessageId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

// IMessageRepository（ドメインの IF）の Exposed 実装（アダプタ）。書き込み経路を担う。
// トランザクションは呼び出し元（UseCase の @Transactional）が開閉するため、
// ここでは transaction {} で囲まず Exposed DSL を直接呼べる。
@Repository
class MessageRepository : IMessageRepository {
    override fun create(content: MessageContent): Message {
        val newId =
            TMessage.insert {
                it[TMessage.content] = content.value
            }[TMessage.id]

        // DB 生成の created_at を読み直し、採番済みの集約として組み立てて返す（識別子戦略 A）。
        return TMessage
            .selectAll()
            .where { TMessage.id eq newId }
            .single()
            .toMessage()
    }

    // Exposed の行(ResultRow) → ドメインの集約。VO に詰め替える。
    private fun ResultRow.toMessage() =
        Message(
            id = MessageId(this[TMessage.id]),
            content = MessageContent(this[TMessage.content]),
            createdAt = this[TMessage.createdAt],
        )
}
