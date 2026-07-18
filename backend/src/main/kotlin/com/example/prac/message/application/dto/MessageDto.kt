package com.example.prac.message.application.dto

import java.time.OffsetDateTime

// DTO: application → presentation へ渡す出力データの形。
// 集約 Message やインフラの Projection とは別物（表示向けの素直な形）。
// Command/Query どちらの結果(Result)にも載る、共通の出力表現。
data class MessageDto(
    val id: Long,
    val content: String,
    val createdAt: OffsetDateTime,
)
