package com.example.prac.message.domain

// 値オブジェクト（Value Object）: メッセージ本文。
// バリデーションを VO の init に閉じ込めることで、「不正な MessageContent は存在し得ない」
// を型で保証する。ドメインの中に不変条件を集約するのが DDD の肝。
@JvmInline
value class MessageContent(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "content must not be blank" }
        require(value.length <= MAX_LENGTH) { "content must be $MAX_LENGTH characters or fewer" }
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}
