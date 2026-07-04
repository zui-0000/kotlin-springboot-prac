package com.example.prac.message

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// @Transactional により、Spring がトランザクションを開閉してくれる。
// そのため Exposed の DSL を transaction {} で囲まずに直接呼べる。
@Service
@Transactional
class MessageService {
    // 一覧取得
    fun list(): List<Message> = Messages.selectAll().map { it.toMessage() }

    // 登録: INSERT で採番された id を取得し、DB 生成の created_at を読み直して返す
    fun create(content: String): Message {
        val newId =
            Messages.insert {
                it[Messages.content] = content
            }[Messages.id]

        return Messages
            .selectAll()
            .where { Messages.id eq newId }
            .single()
            .toMessage()
    }

    // Exposed の行(ResultRow)を Message に詰め替える
    private fun ResultRow.toMessage() =
        Message(
            id = this[Messages.id],
            content = this[Messages.content],
            createdAt = this[Messages.createdAt],
        )
}
