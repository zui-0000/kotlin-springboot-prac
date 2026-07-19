package com.example.prac.message.infrastructure

import com.example.prac.message.application.query.IMessageQueryService
import com.example.prac.message.application.query.MessageProjection
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

// IMessageQueryService（読み取り用インターフェース）の Exposed 実装（アダプタ）。読み取り経路を担う。
// 集約やドメインを経由せず、DB から直接 Projection に詰めるのが CQRS の読み側の旨味。
// 将来は絞り込み・並び替え・ページング・JOIN 集計などをここで自由に最適化できる。
//
// 【型の境界】Exposed の uuid 列(kotlin.uuid.Uuid) → アプリ正準の java.util.UUID へ変換する。
@Repository
class MessageQueryService : IMessageQueryService {
    override fun listAll(): List<MessageProjection> = TMessage.selectAll().map { it.toProjection() }

    // Exposed の行(ResultRow) → 読みモデル(Projection)。VO を通さずプリミティブで詰める。
    @OptIn(ExperimentalUuidApi::class)
    private fun ResultRow.toProjection() =
        MessageProjection(
            id = this[TMessage.id].toJavaUuid(),
            userId = this[TMessage.userId].toJavaUuid(),
            content = this[TMessage.content],
            createdAt = this[TMessage.createdAt],
            updatedAt = this[TMessage.updatedAt],
        )
}
