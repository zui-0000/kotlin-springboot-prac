package com.example.prac.message.application.command

import com.example.prac.message.application.dto.MessageDto
import com.example.prac.message.domain.IMessageRepository
import com.example.prac.message.domain.Message
import com.example.prac.message.domain.MessageContent
import org.springframework.stereotype.Service

// Command Handler: 純粋な書き込みCRUD。認可は持たない（認可は UseCase 層の責務）。
// トランザクション境界は呼び出し元の UseCase(@Transactional) にあるため、ここには付けない。
// 素の入力を VO(MessageContent) に変換（この時点で検証が走る）→ 永続化 → DTO で結果を返す。
@Service
class CreateMessageCommandHandler(
    private val repository: IMessageRepository,
) {
    fun handle(command: CreateMessageCommand): CreateMessageCommandResult {
        val message = repository.create(MessageContent(command.content))
        return CreateMessageCommandResult(message.toDto())
    }

    // ドメイン集約 → DTO
    private fun Message.toDto() =
        MessageDto(
            id = id.value,
            content = content.value,
            createdAt = createdAt,
        )
}
