package com.answufeng.db.demo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int,
    val tags: List<String> = emptyList(),
    val createdAt: Date = Date()
)
