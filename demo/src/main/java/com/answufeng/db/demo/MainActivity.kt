package com.answufeng.db.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
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

        db = DatabaseManager.getOrCreate<AppDatabase>(this, "demo.db") {
            fallbackToDestructiveMigration()
        }

        container.addView(button("Insert User") { insertUser() })
        container.addView(button("Batch Insert") { batchInsert() })
        container.addView(button("Query All") { queryUsers() })
        container.addView(button("Query By ID") { queryById() })
        container.addView(button("Upsert User") { upsertUser() })
        container.addView(button("Count Users") { countUsers() })
        container.addView(button("Delete All") { deleteUsers() })
        container.addView(button("Test DbResult") { testDbResult() })
        container.addView(button("Test withTx") { testWithTx() })
        container.addView(button("Test BatchExecute") { testBatchExecute() })
        container.addView(button("Test DbResult flatMap") { testFlatMap() })
        container.addView(button("Observe Flow") { observeFlow() })
    }

    override fun onDestroy() {
        super.onDestroy()
        DatabaseManager.release("demo.db")
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        val scrollView = tvLog.parent as? ScrollView
        scrollView?.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun insertUser() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val user = User(name = "User-${System.currentTimeMillis() % 1000}", age = (20..60).random())
            val id = dao.insert(user)
            log("Inserted: $user, id=$id")
        }
    }

    private fun batchInsert() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "Batch-$it", age = (20..30).random())
            }
            val ids = dao.insertAll(users)
            log("Batch inserted ${ids.size} users, ids=$ids")
        }
    }

    private fun queryUsers() {
        lifecycleScope.launch {
            val users = db.userDao().getAll()
            log("Users: ${users.size}")
            users.forEach { log("  $it") }
        }
    }

    private fun queryById() {
        lifecycleScope.launch {
            val user = db.userDao().getById(1)
            if (user != null) {
                log("Found: $user")
            } else {
                log("User not found (id=1)")
            }
        }
    }

    private fun upsertUser() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val user = User(id = 1, name = "Upserted-${System.currentTimeMillis() % 1000}", age = 99)
            val id = dao.upsert(user)
            log("Upserted: $user, result=$id")
        }
    }

    private fun countUsers() {
        lifecycleScope.launch {
            val count = db.userDao().count()
            log("User count: $count")
        }
    }

    private fun deleteUsers() {
        lifecycleScope.launch {
            db.userDao().deleteAll()
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

    private fun testWithTx() {
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

    private fun testBatchExecute() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "BatchExec-$it", age = (20..30).random())
            }
            val result = db.batchExecute(users) { user ->
                dao.insert(user)
            }
            when (result) {
                is BatchResult.Skipped -> log("BatchExecute success: ${result.successCount}, failed: ${result.failedCount}")
                is BatchResult.AllOrNothing -> result.result.onSuccess { log("BatchExecute all success: $it") }
                    .onFailure { log("BatchExecute failed: ${it.message}") }
            }
        }
    }

    private fun testFlatMap() {
        lifecycleScope.launch {
            val userResult = dbResultOf { db.userDao().getById(1) }
            val result = userResult.flatMap { user ->
                DbResult.Success(listOf(user))
            }
            result.onSuccess { log("flatMap result: ${it.size} users") }
                .onFailure { log("flatMap failure: ${it.message}") }
        }
    }

    private fun observeFlow() {
        lifecycleScope.launch {
            db.userDao().observeAll()
                .asDbResultWithLoading()
                .collect { result ->
                    result.fold(
                        onLoading = { log("[Flow] Loading...") },
                        onSuccess = { log("[Flow] Received: ${it.size} users") },
                        onFailure = { log("[Flow] Error: ${it.message}") }
                    )
                }
        }
    }
}
