package com.example.prac.message.application.query

import java.time.OffsetDateTime

// Projection: QueryService(読み取り)が返す "生の読みモデル"。
// DB の SELECT 結果をそのまま表し、ドメイン集約は通さない（CQRS の読み経路）。
//
// 【記事との差異・意図的】記事は Projection を infrastructure に置くが、本プロジェクトでは
// application の IMessageQueryService が返す型のため、依存の向き(infra → application)を
// 守るべく application 側に置く。これが唯一の意図的な逸脱。
data class MessageProjection(
    val id: Long,
    val content: String,
    val createdAt: OffsetDateTime,
)
