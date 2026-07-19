package com.example.prac.message.domain

import java.util.UUID

// 値オブジェクト（Value Object）: メッセージの識別子。
// 素の UUID を持ち歩くと「どの UUID がどの id か」を取り違える。VO にして型で守る。
// 値は UUIDv7（DB 側の DEFAULT uuidv7() で採番）。@JvmInline なので実行時は UUID と同じ（ラップ無し）。
@JvmInline
value class MessageId(
    val value: UUID,
)
