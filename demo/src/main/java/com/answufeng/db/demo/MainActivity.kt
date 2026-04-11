package com.answufeng.db.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.answufeng.db.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = TextView(this).apply { textSize = 14f }
        val container = findViewById<LinearLayout>(R.id.container)

        container.addView(tvLog)

        db = BrickDatabase.build(this, "demo.db") {
            fallbackToDestructiveMigration()
            allowMainThreadQueries()
        }

        container.addView(button("Insert User") { insertUser() })
        container.addView(button("Query All Users") { queryUsers() })
        container.addView(button("Delete All Users") { deleteUsers() })
        container.addView(button("Test DbResult") { testDbResult() })
        container.addView(button("Test Transaction") { testTransaction() })
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
    }

    private fun insertUser() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val user = User(name = "User-${System.currentTimeMillis() % 1000}", age = (20..60).random())
            dao.insert(user)
            log("Inserted: $user")
        }
    }

    private fun queryUsers() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val users = dao.getAll()
            log("Users: ${users.size}")
            users.forEach { log("  $it") }
        }
    }

    private fun deleteUsers() {
        lifecycleScope.launch {
            val dao = db.userDao()
            dao.deleteAll()
            log("All users deleted")
        }
    }

    private fun testDbResult() {
        lifecycleScope.launch {
            val result = dbResultOf { db.userDao().getAll() }
            result.fold(
                onLoading = { log("Loading...") },
                onSuccess = { log("DbResult Success: ${it.size} users") },
                onFailure = { log("DbResult Failure: ${it.message}") }
            )
        }
    }

    private fun testTransaction() {
        lifecycleScope.launch {
            val result = db.safeTransaction {
                val dao = userDao()
                dao.insert(User(name = "Tx1", age = 25))
                dao.insert(User(name = "Tx2", age = 30))
                dao.getAll()
            }
            result.onSuccess { log("Transaction success: ${it.size} users") }
            result.onFailure { log("Transaction failed: ${it.message}") }
        }
    }
}
