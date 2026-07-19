package com.example.prac.message.application.query

// 読み取り用のインターフェース（ヘキサゴナルでいう "ポート"）。
// application が「読み取りに何を求めるか」を定義し、実装（Exposed の最適化クエリ）は
// infrastructure 層のアダプタに置く。書き込み側の IMessageRepository と対称の構造。
// 返すのは Projection(生の読みモデル)。ドメインの集約は一切通さない（CQRS の読み経路）。
interface IMessageQueryService {
    // ページ内の1件分を offset/limit で取得する。
    fun listPaged(
        limit: Int,
        offset: Long,
    ): List<MessageProjection>

    // 総件数（pagination の totalCount 用）。
    fun count(): Long
}
