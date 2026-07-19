package com.example.prac.message.application.query

import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.application.dto.PaginationDto

// QueryResult: 読み取りユースケースの出力。ページ内の DTO リストとページネーション情報を保持する。
// meta(respondedAt) はレスポンス生成時の関心事のため presentation 側で付与する（ここには持たない）。
data class ListMessagesQueryResult(
    val messages: List<MessageDto>,
    val pagination: PaginationDto,
)
