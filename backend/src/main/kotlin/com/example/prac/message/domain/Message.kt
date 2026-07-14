package com.example.prac.message.domain

import java.time.OffsetDateTime

// 集約ルート（Aggregate Root）: "すでに存在するメッセージ" を表すドメインモデル。
// 識別子戦略は「A: リポジトリが採番後の集約を返す」を採用（docs/21 参照）。
// そのため id は常に確定しており、nullable にはしない。
// フレームワーク非依存の純粋な data class（Spring / Exposed を一切 import しない）。
data class Message(
    val id: MessageId,
    val content: MessageContent,
    val createdAt: OffsetDateTime,
)
