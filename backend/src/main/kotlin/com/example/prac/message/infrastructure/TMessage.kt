package com.example.prac.message.infrastructure

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// t_message テーブルの Exposed 定義。infrastructure 層の実装詳細。
// スキーマの「正」は Flyway。ここはそれに"合わせて"書く。
// テーブル名は命名規則（トランザクション系=t_・単数形）に従い t_message。
object TMessage : Table("t_message") {
    // id は DB 側の DEFAULT uuidv7()（PostgreSQL 18 ネイティブ）で採番されるため databaseGenerated()。
    val id = uuid("id").databaseGenerated()

    // 所有者(User)への外部キー。FK 制約は Flyway 側が持つため、ここでは素の uuid 列として写す。
    val userId = uuid("user_id")
    val content = text("content")

    // created_at は DB 側の DEFAULT now() で入る。databaseGenerated() で INSERT 時に Exposed が触らない。
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()

    // updated_at も INSERT 時は DB の DEFAULT now()。UPDATE 実装時にアプリが now() をセットする（後日）。
    val updatedAt = timestampWithTimeZone("updated_at").databaseGenerated()

    override val primaryKey = PrimaryKey(id)
}
