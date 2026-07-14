package com.example.prac.message.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Query Handler: 読み取りユースケースの本体。
// ポート経由で View を取得して返すだけ。ドメイン・集約・リポジトリを通さないのが CQRS の読み経路。
// readOnly = true で読み取り専用トランザクションにする（最適化のヒント）。
@Service
@Transactional(readOnly = true)
class ListMessagesQueryHandler(
    private val queryPort: MessageQueryPort,
) {
    fun handle(query: ListMessagesQuery): List<MessageView> = queryPort.listAll()
}
