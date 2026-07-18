package com.example.prac.message.infrastructure

import com.example.prac.message.application.query.IMessageQueryService
import com.example.prac.message.application.query.MessageProjection
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

// IMessageQueryService（読み取り用インターフェース）の Exposed 実装（アダプタ）。読み取り経路を担う。
// 集約やドメインを経由せず、DB から直接 Projection に詰めるのが CQRS の読み側の旨味。
// 将来は絞り込み・並び替え・ページング・JOIN 集計などをここで自由に最適化できる。
@Repository
class MessageQueryService : IMessageQueryService {
    override fun listAll(): List<MessageProjection> = TMessage.selectAll().map { it.toProjection() }

    // Exposed の行(ResultRow) → 読みモデル(Projection)。VO を通さずプリミティブで詰める。
    private fun ResultRow.toProjection() =
        MessageProjection(
            id = this[TMessage.id],
            content = this[TMessage.content],
            createdAt = this[TMessage.createdAt],
        )
}
