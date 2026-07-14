package com.example.prac.message.domain

// 値オブジェクト（Value Object）: メッセージの識別子。
// 素の Long を持ち歩くと「どの Long がどの id か」を取り違える。VO にして型で守る。
// @JvmInline value class なので実行時は Long と同じ（ラップのオーバーヘッドなし）。
@JvmInline
value class MessageId(
    val value: Long,
)
