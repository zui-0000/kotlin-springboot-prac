package com.example.prac.message.infrastructure

import com.example.prac.message.application.query.MessageQueryPort
import com.example.prac.message.application.query.MessageView
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

// MessageQueryPort（読み取りポート）の Exposed 実装。読み取り経路を担う。
// 集約やドメインを経由せず、DB から直接 View に詰めるのが CQRS の読み側の旨味。
// 将来は絞り込み・並び替え・ページング・JOIN 集計などをここで自由に最適化できる。
@Repository
class ExposedMessageQueryPort : MessageQueryPort {
    override fun listAll(): List<MessageView> = Messages.selectAll().map { it.toView() }

    // Exposed の行(ResultRow) → 読み取りモデル(View)。VO を通さずプリミティブで詰める。
    private fun ResultRow.toView() =
        MessageView(
            id = this[Messages.id],
            content = this[Messages.content],
            createdAt = this[Messages.createdAt],
        )
}
