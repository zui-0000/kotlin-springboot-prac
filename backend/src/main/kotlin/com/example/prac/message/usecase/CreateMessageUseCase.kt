package com.example.prac.message.usecase

import com.example.prac.generated.model.CreateMessageRequest
import com.example.prac.message.application.command.CreateMessageCommand
import com.example.prac.message.application.command.CreateMessageCommandHandler
import com.example.prac.message.application.command.CreateMessageCommandResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// UseCase 層: ビジネスフロー + 認可制御を担う。
// 「認可チェック → Request を Command へ変換 → Handler 呼び出し」の順で組み立てる。
// トランザクション境界はここ（複数 Handler を束ねても1トランザクションで実行するため）。
// ※ 記事に倣い Request→Command 変換を UseCase が担う。その代償として生成 Request 型に依存する。
@Service
class CreateMessageUseCase(
    private val handler: CreateMessageCommandHandler,
) {
    @Transactional
    fun execute(request: CreateMessageRequest): CreateMessageCommandResult {
        // 認可: 認証基盤(auth/・security/)の導入後にここで権限チェックを行う（現状は無し）。
        val command = CreateMessageCommand(content = request.content)
        return handler.handle(command)
    }
}
