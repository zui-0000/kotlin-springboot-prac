package com.example.prac.message.application.command

// Command: 「メッセージを作りたい」という"意図"を表す入力データ。
// 外側（presentation）から来る素の値を運ぶだけ。VO への変換・検証は Handler / ドメインが行う。
data class CreateMessageCommand(
    val content: String,
)
