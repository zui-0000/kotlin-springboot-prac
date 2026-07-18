package com.example.prac.message.application.command

import com.example.prac.message.domain.IMessageRepository
import com.example.prac.message.domain.Message
import com.example.prac.message.domain.MessageContent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Command Handler: 書き込みユースケースの本体。トランザクション境界はここ。
// 素の入力を VO(MessageContent) に変換（この時点で検証が走る）し、リポジトリ経由で永続化する。
// 生成された集約を返し、呼び出し元（Controller）が DTO へ変換する。
@Service
@Transactional
class CreateMessageCommandHandler(
    private val repository: IMessageRepository,
) {
    fun handle(command: CreateMessageCommand): Message = repository.create(MessageContent(command.content))
}
