package com.example.prac.message.application.query

// Query: 「メッセージ一覧が欲しい」という読み取りの"意図" + ページング指定。
// currentPage / perPage は未指定(null)を許し、既定値は Handler で適用する。
data class ListMessagesQuery(
    val currentPage: Int?,
    val perPage: Int?,
)
