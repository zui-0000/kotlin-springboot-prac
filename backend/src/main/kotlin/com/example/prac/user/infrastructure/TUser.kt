package com.example.prac.user.infrastructure

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// t_user テーブルの Exposed 定義。infrastructure 層の実装詳細。
// スキーマの「正」は Flyway。ここはそれに"合わせて"書く（制約 UNIQUE / FK は Flyway 側が持つ）。
// id は DB 側の DEFAULT uuidv7()（PostgreSQL 18 ネイティブ）で採番されるため databaseGenerated()。
object TUser : Table("t_user") {
    val id = uuid("id").databaseGenerated()
    val name = text("name")
    val email = text("email")

    // created_at / updated_at は DB 側の DEFAULT now() で入る（INSERT 時）。
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").databaseGenerated()

    override val primaryKey = PrimaryKey(id)
}
