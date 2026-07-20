package com.example.prac.message.application.command

import com.example.prac.generated.model.CommonResponseMeta
import com.example.prac.generated.model.MessageCreateResponse
import com.example.prac.generated.model.MessageCreateResult
import com.example.prac.message.domain.IMessageRepository
import com.example.prac.message.domain.model.Message
import com.example.prac.message.domain.model.MessageContent
import com.example.prac.user.domain.model.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

// Command Handler = 書き込みユースケース本体。トランザクション境界はここ。
// 出力は生成スキーマ型(MessageCreateResponse=エンベロープ)を直接組んで返す方針。
// これにより application → generated の依存を許容する代わりに、出力 DTO の二重定義を無くす。
// （domain(Message/VO)は generated 非依存を維持。変換はこの handler が担う）
@Service
@Transactional
class CreateMessageCommandHandler(
    private val repository: IMessageRepository,
) {
    fun handle(command: CreateMessageCommand): MessageCreateResponse {
        val message = repository.create(UserId(command.userId), MessageContent(command.content))
        return MessageCreateResponse(
            result = message.toResult(),
            meta = CommonResponseMeta(respondedAt = OffsetDateTime.now()),
        )
    }

    // ドメイン集約 → 生成レスポンスの Result
    private fun Message.toResult() =
        MessageCreateResult(
            id = id.value,
            userId = userId.value,
            content = content.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
