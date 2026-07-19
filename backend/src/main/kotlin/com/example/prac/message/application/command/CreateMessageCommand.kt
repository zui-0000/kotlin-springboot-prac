package com.example.prac.message.application.command

import java.util.UUID

// Command: 「メッセージを作りたい」という"意図"を表す入力データ。
// 外側（presentation）から来る素の値を運ぶだけ。VO への変換・検証は Handler / ドメインが行う。
// userId は本来ログイン中のユーザー(認証コンテキスト)から得るべき値だが、認証未実装のため
// 当面はリクエスト由来の素の UUID を運ぶ暫定形（認証実装時に auth 由来へ差し替える）。
data class CreateMessageCommand(
    val userId: UUID,
    val content: String,
)
