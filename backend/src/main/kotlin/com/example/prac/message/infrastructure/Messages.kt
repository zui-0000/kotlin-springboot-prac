package com.example.prac.message.infrastructure

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// messages テーブルの Exposed 定義。infrastructure 層の実装詳細。
// スキーマの「正」は Flyway (V*__create_messages_table.sql)。ここはそれに"合わせて"書く。
object Messages : Table("messages") {
    val id = long("id").autoIncrement()
    val content = text("content")

    // created_at は DB 側の DEFAULT now() で入る。
    // databaseGenerated() で「DB が生成する列」と伝え、INSERT 時に Exposed が触らないようにする。
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()

    override val primaryKey = PrimaryKey(id)
}
