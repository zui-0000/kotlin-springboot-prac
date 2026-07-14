package com.example.prac.message.application.query

import java.time.OffsetDateTime

// 読み取り専用モデル（Read Model）。ドメインの集約 Message とは"別物"。
// 集約は不変条件を守るための型、View は画面/API に返すための型——CQRS では役割で分ける。
// そのため VO ではなくプリミティブでよい（守るべきロジックが無いため）。
data class MessageView(
    val id: Long,
    val content: String,
    val createdAt: OffsetDateTime,
)
