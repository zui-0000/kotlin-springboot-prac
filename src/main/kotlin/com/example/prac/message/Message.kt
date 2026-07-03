package com.example.prac.message

import java.time.OffsetDateTime

// API のレスポンス／ドメイン表現。Exposed の行(ResultRow)から詰め替えて使う。
// JPA と違い、ただの data class でよい（open 縛りや no-arg プラグインが不要）。
data class Message(
    val id: Long,
    val content: String,
    val createdAt: OffsetDateTime,
)
