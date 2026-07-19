package com.example.prac.message.application.dto

// DTO: ページネーション情報（データセットに関する情報）。
// 「レスポンス自体に関する情報」である meta とは性質が異なるため分離する（テンプレの設計方針）。
data class PaginationDto(
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val perPage: Int,
)
