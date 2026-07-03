package com.example.prac.message

import org.springframework.data.jpa.repository.JpaRepository

// JpaRepository を継承するだけで基本的な CRUD メソッドが手に入る
interface MessageRepository : JpaRepository<Message, Long>
