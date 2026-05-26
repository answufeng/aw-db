package com.answufeng.db.demo

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.answufeng.db.AwConverters

@Database(
    entities = [User::class, LoginHistory::class],
    version = 2
)
@TypeConverters(AwConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun loginHistoryDao(): LoginHistoryDao
}
