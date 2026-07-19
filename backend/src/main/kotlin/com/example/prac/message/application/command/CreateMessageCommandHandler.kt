package com.example.prac.message.application.command

import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.domain.IMessageRepository
import com.example.prac.message.domain.Message
import com.example.prac.message.domain.MessageContent
import com.example.prac.user.domain.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Command Handler = 書き込みユースケース本体。トランザクション境界はここ。
// 素の入力を VO(UserId / MessageContent) に変換（この時点で検証が走る）→ 永続化 → DTO で結果を返す。
// 複数の repository / domain service を束ねる協調もここに書いてよい（ただし他の Handler は呼ばない）。
@Service
@Transactional
class CreateMessageCommandHandler(
    private val repository: IMessageRepository,
) {
    fun handle(command: CreateMessageCommand): CreateMessageCommandResult {
        val message = repository.create(UserId(command.userId), MessageContent(command.content))
        return CreateMessageCommandResult(message.toDto())
    }

    // ドメイン集約 → DTO
    private fun Message.toDto() =
        MessageDto(
            id = id.value,
            userId = userId.value,
            content = content.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
