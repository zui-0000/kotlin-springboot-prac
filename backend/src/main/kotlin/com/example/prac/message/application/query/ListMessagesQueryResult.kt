package com.example.prac.message.application.query

import com.example.prac.message.application.dto.MessageDto

// QueryResult: 読み取りユースケースの出力。DTO のリストを保持する。
// QueryHandler が Projection → DTO に詰め替えた結果を、この型に包んで UseCase へ返す。
data class ListMessagesQueryResult(
    val messages: List<MessageDto>,
)
