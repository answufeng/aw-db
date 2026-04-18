package com.answufeng.db.demo

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.answufeng.db.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView
    private var flowJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        db = DatabaseManager.getOrCreate<AppDatabase>(this, "demo.db") {
            fallbackToDestructiveMigration()
        }
        log("数据库初始化完成: demo.db")

        findViewById<Button>(R.id.btnInsert).setOnClickListener { insertUser() }
        findViewById<Button>(R.id.btnBatchInsert).setOnClickListener { batchInsert() }
        findViewById<Button>(R.id.btnQueryAll).setOnClickListener { queryUsers() }
        findViewById<Button>(R.id.btnQueryById).setOnClickListener { queryById() }
        findViewById<Button>(R.id.btnUpsert).setOnClickListener { upsertUser() }
        findViewById<Button>(R.id.btnCount).setOnClickListener { countUsers() }
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { deleteUsers() }
        findViewById<Button>(R.id.btnInsertOrIgnore).setOnClickListener { insertOrIgnore() }
        findViewById<Button>(R.id.btnUpdate).setOnClickListener { updateUser() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { deleteUser() }
        findViewById<Button>(R.id.btnDbResult).setOnClickListener { testDbResult() }
        findViewById<Button>(R.id.btnTransaction).setOnClickListener { testWithTx() }
        findViewById<Button>(R.id.btnBatchExecute).setOnClickListener { testBatchExecute() }
        findViewById<Button>(R.id.btnObserveFlow).setOnClickListener { observeFlow() }
        findViewById<Button>(R.id.btnDebugHelper).setOnClickListener { testDebugHelper() }
        findViewById<Button>(R.id.btnPagedResult).setOnClickListener { testPagedResult() }
        findViewById<Button>(R.id.btnBackup).setOnClickListener { testBackup() }
        findViewById<Button>(R.id.btnGetOrNull).setOnClickListener { testGetOrNull() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }
    }

    override fun onDestroy() {
        super.onDestroy()
        flowJob?.cancel()
        DatabaseManager.release("demo.db")
        log("数据库连接已释放")
    }

    private fun log(msg: String) {
        val timestamp = timeFormat.format(Date())
        tvLog.append("[$timestamp] $msg\n")
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
            val tags = listOf("tag-${(1..10).random()}", "tag-${(1..10).random()}")
            val user = User(name = "用户-${System.currentTimeMillis() % 1000}", age = (20..60).random(), tags = tags)
            val id = dao.insert(user)
            log("插入成功: $user, ID=$id")
        }
    }

    private fun batchInsert() {
        lifecycleScope.launch {
            log("开始批量插入...")
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "批量-$it", age = (20..30).random(), tags = listOf("batch-$it"))
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

    private fun insertOrIgnore() {
        lifecycleScope.launch {
            log("开始测试 insertOrIgnore...")
            val dao = db.userDao()
            val user1 = User(id = 1, name = "忽略测试1", age = 25)
            val id1 = dao.insertOrIgnore(user1)
            log("首次 insertOrIgnore: ID=$id1")
            val user2 = User(id = 1, name = "忽略测试2", age = 30)
            val id2 = dao.insertOrIgnore(user2)
            log("重复 insertOrIgnore: ID=$id2 (冲突忽略，返回 -1)")
        }
    }

    private fun updateUser() {
        lifecycleScope.launch {
            log("开始更新用户...")
            val dao = db.userDao()
            val user = dao.getById(1)
            if (user != null) {
                val updated = user.copy(name = "已更新-${System.currentTimeMillis() % 1000}")
                val rows = dao.update(updated)
                log("更新成功: $updated, 影响行数=$rows")
            } else {
                log("未找到 ID=1 的用户，无法更新")
            }
        }
    }

    private fun deleteUser() {
        lifecycleScope.launch {
            log("开始删除单条用户...")
            val dao = db.userDao()
            val user = dao.getById(1)
            if (user != null) {
                val rows = dao.delete(user)
                log("删除成功: $user, 影响行数=$rows")
            } else {
                log("未找到 ID=1 的用户，无法删除")
            }
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

            val lazyResult: DbResult<String> = DbResult.Failure(RuntimeException("test"))
            val recovered = lazyResult.getOrElse { "默认值（惰性求值）" }
            log("getOrElse 惰性求值: $recovered")
        }
    }

    private fun testWithTx() {
        lifecycleScope.launch {
            log("开始测试事务...")
            val result = db.safeTransaction {
                val dao = userDao()
                dao.insert(User(name = "事务1", age = 25, tags = listOf("tx")))
                dao.insert(User(name = "事务2", age = 30, tags = listOf("tx")))
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
        if (flowJob?.isActive == true) {
            flowJob?.cancel()
            flowJob = null
            log("Flow 观察已停止")
            return
        }
        flowJob = lifecycleScope.launch {
            log("开始观察 Flow（再次点击停止）...")
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

    private fun testDebugHelper() {
        lifecycleScope.launch {
            log("开始测试 DbDebugHelper...")
            val tables = db.tableList()
            log("表列表: $tables")
            tables.forEach { table ->
                val count = db.rowCount(table)
                log("  $table: $count 行")
            }
            val columns = db.tableSchema("User")
            log("User 表结构:")
            columns.forEach { col ->
                log("  ${col.name} ${col.type}${if (col.notNull) " NOT NULL" else ""}${col.defaultValue?.let { " DEFAULT $it" } ?: ""}")
            }
            log("引用计数: ${DatabaseManager.getReferenceCount("demo.db")}")
            log("是否被管理: ${DatabaseManager.isManaged("demo.db")}")
        }
    }

    private fun testPagedResult() {
        lifecycleScope.launch {
            log("开始测试 PagedResult 手动分页...")
            val dao = db.userDao()
            val pageSize = 3
            val page = 0
            val items = dao.getPage(pageSize, page * pageSize)
            val total = dao.count()
            val result = items.toPagedResult(page, pageSize, total)
            log("分页结果: 第${result.page}页, ${result.items.size}条/页, 总共${result.total}条, hasMore=${result.hasMore}, totalPages=${result.totalPages}")
            result.items.forEach { log("  $it") }
        }
    }

    private fun testBackup() {
        lifecycleScope.launch {
            log("开始测试数据库备份...")
            try {
                val backupDir = File(getExternalFilesDir(null), "backup")
                val backupFile = File(backupDir, "demo_backup.db")
                db.backupTo(backupFile)
                log("备份成功: ${backupFile.absolutePath} (${backupFile.length()} bytes)")
            } catch (e: Exception) {
                log("备份失败: ${e.message}")
            }
        }
    }

    private fun testGetOrNull() {
        val existing = DatabaseManager.getOrNull<AppDatabase>("demo.db")
        log("getOrNull('demo.db'): ${existing != null}")

        val notExisting = DatabaseManager.getOrNull<AppDatabase>("nonexistent.db")
        log("getOrNull('nonexistent.db'): ${notExisting != null}")

        val defaultName = DatabaseManager.getOrCreate<AppDatabase>(this) {
            fallbackToDestructiveMigration()
        }
        log("getOrCreate 默认名称: ${AppDatabase::class.java.simpleName}")
        DatabaseManager.release(AppDatabase::class.java.simpleName)
    }
}
