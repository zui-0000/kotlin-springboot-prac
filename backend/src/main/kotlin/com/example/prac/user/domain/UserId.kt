package com.example.prac.user.domain

import java.util.UUID

// 値オブジェクト（Value Object）: ユーザーの識別子。
// 素の UUID を持ち歩くと取り違えるため VO で型付けする。値は UUIDv7（DB 側の DEFAULT uuidv7() で採番）。
// message など他コンテキストからは、この UserId "だけ" を参照する（User 集約の実体は持ち込まない）。
@JvmInline
value class UserId(
    val value: UUID,
)
