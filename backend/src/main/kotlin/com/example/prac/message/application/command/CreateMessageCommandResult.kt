package com.example.prac.message.application.command

import com.example.prac.message.application.dto.MessageDto

// CommandResult: 書き込みユースケースの出力。作成されたメッセージを DTO で返す。
// Command(入力) と対になる出力型。UseCase を経由して presentation へ返る。
data class CreateMessageCommandResult(
    val message: MessageDto,
)
