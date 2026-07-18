package com.example.prac.message.infrastructure

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// t_message テーブルの Exposed 定義。infrastructure 層の実装詳細。
// スキーマの「正」は Flyway。ここはそれに"合わせて"書く。
// テーブル名は命名規則（トランザクション系=t_・単数形）に従い t_message。
object TMessage : Table("t_message") {
    val id = long("id").autoIncrement()
    val content = text("content")

    // created_at は DB 側の DEFAULT now() で入る。
    // databaseGenerated() で「DB が生成する列」と伝え、INSERT 時に Exposed が触らないようにする。
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()

    override val primaryKey = PrimaryKey(id)
}
