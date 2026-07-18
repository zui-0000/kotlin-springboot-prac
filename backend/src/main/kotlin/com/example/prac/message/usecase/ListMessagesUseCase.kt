package com.example.prac.message.usecase

import com.example.prac.message.application.query.ListMessagesQuery
import com.example.prac.message.application.query.ListMessagesQueryHandler
import com.example.prac.message.application.query.ListMessagesQueryResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// UseCase 層(読み取り): 認可制御 → Query 組み立て → Query Handler 呼び出し。
@Service
class ListMessagesUseCase(
    private val handler: ListMessagesQueryHandler,
) {
    @Transactional(readOnly = true)
    fun execute(): ListMessagesQueryResult {
        // 認可: 認証基盤の導入後にここで権限チェックを行う（現状は無し）。
        val query = ListMessagesQuery() // 将来フィルタ・ページング条件が来たらここで組み立てる
        return handler.handle(query)
    }
}
