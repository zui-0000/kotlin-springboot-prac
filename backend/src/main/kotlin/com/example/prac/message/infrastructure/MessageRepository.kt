package com.example.prac.message.infrastructure

import com.example.prac.message.domain.IMessageRepository
import com.example.prac.message.domain.Message
import com.example.prac.message.domain.MessageContent
import com.example.prac.message.domain.MessageId
import com.example.prac.user.domain.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

// IMessageRepository（ドメインの IF）の Exposed 実装（アダプタ）。書き込み経路を担う。
// トランザクションは呼び出し元（Handler の @Transactional）が開閉するため、
// ここでは transaction {} で囲まず Exposed DSL を直接呼べる。
//
// 【型の境界】Exposed 1.x の uuid 列は Kotlin ネイティブの kotlin.uuid.Uuid(experimental)を扱う。
// アプリの正準型は java.util.UUID なので、この "アダプタ内でだけ" 相互変換する
// （toKotlinUuid / toJavaUuid）。experimental API はここに封じ込め、domain には漏らさない。
@Repository
class MessageRepository : IMessageRepository {
    @OptIn(ExperimentalUuidApi::class)
    override fun create(
        userId: UserId,
        content: MessageContent,
    ): Message {
        val newId =
            TMessage.insert {
                it[TMessage.userId] = userId.value.toKotlinUuid()
                it[TMessage.content] = content.value
            }[TMessage.id]

        // DB 生成の id / created_at / updated_at を読み直し、確定済みの集約として返す（識別子戦略 A）。
        return TMessage
            .selectAll()
            .where { TMessage.id eq newId }
            .single()
            .toMessage()
    }

    // Exposed の行(ResultRow) → ドメインの集約。VO に詰め替える（uuid は java.util.UUID へ変換）。
    @OptIn(ExperimentalUuidApi::class)
    private fun ResultRow.toMessage() =
        Message(
            id = MessageId(this[TMessage.id].toJavaUuid()),
            userId = UserId(this[TMessage.userId].toJavaUuid()),
            content = MessageContent(this[TMessage.content]),
            createdAt = this[TMessage.createdAt],
            updatedAt = this[TMessage.updatedAt],
        )
}
