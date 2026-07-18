package com.example.prac.message.domain

// リポジトリ interface。ドメインが「永続化に何を求めるか」を定義し、"所有" する。
// 実装（Exposed）は infrastructure 層に置く（依存性逆転: infra → domain の向き）。
// 書き込み経路（Command）専用。読み取りは IMessageQueryService が担う（CQRS）。
interface IMessageRepository {
    // 本文を受け取り、DB で採番された id / created_at を含む確定済みの集約を返す。
    fun create(content: MessageContent): Message
}
