package com.answufeng.db.demo

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.answufeng.db.*
import kotlinx.coroutines.launch

/**
 * aw-db 库功能演示
 * 包含：基本CRUD、事务、批量操作、Flow观察、DbResult包装
 */
class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 绑定视图
        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        // 初始化数据库
        db = DatabaseManager.getOrCreate<AppDatabase>(this, "demo.db") {
            fallbackToDestructiveMigration()
        }
        log("数据库初始化完成: demo.db")

        // 绑定按钮事件
        findViewById<Button>(R.id.btnInsert).setOnClickListener { insertUser() }
        findViewById<Button>(R.id.btnBatchInsert).setOnClickListener { batchInsert() }
        findViewById<Button>(R.id.btnQueryAll).setOnClickListener { queryUsers() }
        findViewById<Button>(R.id.btnQueryById).setOnClickListener { queryById() }
        findViewById<Button>(R.id.btnUpsert).setOnClickListener { upsertUser() }
        findViewById<Button>(R.id.btnCount).setOnClickListener { countUsers() }
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { deleteUsers() }
        findViewById<Button>(R.id.btnDbResult).setOnClickListener { testDbResult() }
        findViewById<Button>(R.id.btnTransaction).setOnClickListener { testWithTx() }
        findViewById<Button>(R.id.btnBatchExecute).setOnClickListener { testBatchExecute() }
        findViewById<Button>(R.id.btnObserveFlow).setOnClickListener { observeFlow() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }
    }

    override fun onDestroy() {
        super.onDestroy()
        DatabaseManager.release("demo.db")
        log("数据库连接已释放")
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwDBDemo", msg)
    }

    private fun clearLog() {
        tvLog.text = ""
        log("日志已清除")
    }

    private fun insertUser() {
        lifecycleScope.launch {
            log("开始插入用户...")
            val dao = db.userDao()
            val user = User(name = "用户-${System.currentTimeMillis() % 1000}", age = (20..60).random())
            val id = dao.insert(user)
            log("插入成功: $user, ID=$id")
        }
    }

    private fun batchInsert() {
        lifecycleScope.launch {
            log("开始批量插入...")
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "批量-$it", age = (20..30).random())
            }
            val ids = dao.insertAll(users)
            log("批量插入成功: ${ids.size}个用户, IDs=$ids")
        }
    }

    private fun queryUsers() {
        lifecycleScope.launch {
            log("开始查询所有用户...")
            val users = db.userDao().getAll()
            log("用户总数: ${users.size}")
            users.forEach { log("  $it") }
        }
    }

    private fun queryById() {
        lifecycleScope.launch {
            log("开始根据ID查询...")
            val user = db.userDao().getById(1)
            if (user != null) {
                log("找到用户: $user")
            } else {
                log("未找到用户 (ID=1)")
            }
        }
    }

    private fun upsertUser() {
        lifecycleScope.launch {
            log("开始更新/插入用户...")
            val dao = db.userDao()
            val user = User(id = 1, name = "更新-${System.currentTimeMillis() % 1000}", age = 99)
            val id = dao.upsert(user)
            log("更新/插入成功: $user, 结果=$id")
        }
    }

    private fun countUsers() {
        lifecycleScope.launch {
            log("开始统计用户数...")
            val count = db.userDao().count()
            log("用户总数: $count")
        }
    }

    private fun deleteUsers() {
        lifecycleScope.launch {
            log("开始删除所有用户...")
            db.userDao().deleteAll()
            log("所有用户已删除")
        }
    }

    private fun testDbResult() {
        lifecycleScope.launch {
            log("开始测试 DbResult...")
            val result = dbResultOf { db.userDao().getAll() }
            result.fold(
                onLoading = { log("加载中...") },
                onSuccess = { log("DbResult 成功: ${it.size}个用户") },
                onFailure = { log("DbResult 失败: ${it.message}") }
            )
        }
    }

    private fun testWithTx() {
        lifecycleScope.launch {
            log("开始测试事务...")
            val result = db.safeTransaction {
                val dao = userDao()
                dao.insert(User(name = "事务1", age = 25))
                dao.insert(User(name = "事务2", age = 30))
                dao.getAll()
            }
            result.onSuccess { log("事务成功: ${it.size}个用户") }
            result.onFailure { log("事务失败: ${it.message}") }
        }
    }

    private fun testBatchExecute() {
        lifecycleScope.launch {
            log("开始测试批量执行...")
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "批量执行-$it", age = (20..30).random())
            }
            val result = db.batchExecute(users) { user ->
                dao.insert(user)
            }
            when (result) {
                is BatchResult.Skipped -> log("批量执行成功: 成功${result.successCount}个, 失败${result.failedCount}个")
                is BatchResult.AllOrNothing -> result.result.onSuccess { log("批量执行全部成功: $it") }
                    .onFailure { log("批量执行失败: ${it.message}") }
            }
        }
    }

    private fun observeFlow() {
        lifecycleScope.launch {
            log("开始观察 Flow...")
            db.userDao().observeAll()
                .asDbResultWithLoading()
                .collect { result ->
                    result.fold(
                        onLoading = { log("[Flow] 加载中...") },
                        onSuccess = { log("[Flow] 收到: ${it.size}个用户") },
                        onFailure = { log("[Flow] 错误: ${it.message}") }
                    )
                }
        }
    }
}
