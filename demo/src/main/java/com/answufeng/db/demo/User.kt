package com.answufeng.db.demo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    val name: String,
    val age: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    override fun toString(): String = "User(id=$id, name=$name, age=$age)"
}
