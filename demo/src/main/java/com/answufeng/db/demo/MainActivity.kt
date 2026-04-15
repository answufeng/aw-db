package com.answufeng.db.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.answufeng.db.*
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * aw-db 库功能演示
 * 包含：基本CRUD、事务、批量操作、Flow观察、分页查询、复杂查询、数据库迁移
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
        log("✅ 数据库初始化完成: demo.db")

        // 功能卡片布局
        setupFunctionCards()
    }

    private fun setupFunctionCards() {
        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)

        // 基本操作卡片
        val basicCard = createCard("基本操作")
        val basicLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        basicLayout.addView(createButton("插入用户", ::insertUser))
        basicLayout.addView(createButton("批量插入", ::batchInsert))
        basicLayout.addView(createButton("查询所有", ::queryUsers))
        basicLayout.addView(createButton("根据ID查询", ::queryById))
        basicLayout.addView(createButton("更新/插入", ::upsertUser))
        basicLayout.addView(createButton("统计用户数", ::countUsers))
        basicLayout.addView(createButton("删除所有", ::deleteUsers))
        basicCard.addView(basicLayout)
        mainLayout.addView(basicCard)

        // 高级功能卡片
        val advancedCard = createCard("高级功能")
        val advancedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        advancedLayout.addView(createButton("事务操作", ::testWithTx))
        advancedLayout.addView(createButton("批量执行", ::testBatchExecute))
        advancedLayout.addView(createButton("DbResult 测试", ::testDbResult))
        advancedLayout.addView(createButton("分页查询", ::testPagination))
        advancedLayout.addView(createButton("复杂查询", ::testComplexQuery))
        advancedLayout.addView(createButton("Flow 观察", ::observeFlow))
        advancedCard.addView(advancedLayout)
        mainLayout.addView(advancedCard)

        // 管理功能卡片
        val manageCard = createCard("管理功能")
        val manageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        manageLayout.addView(createButton("数据库信息", ::showDatabaseInfo))
        manageLayout.addView(createButton("清除日志", ::clearLog))
        manageLayout.addView(createButton("释放连接", ::releaseConnection))
        manageCard.addView(manageLayout)
        mainLayout.addView(manageCard)
    }

    private fun createCard(title: String): MaterialCardView {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setPadding(20, 20, 20, 20)

            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setPadding(0, 0, 0, 12)
            })
        }
    }

    private fun createButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            setOnClickListener { onClick() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DatabaseManager.release("demo.db")
        log("🔚 数据库连接已释放")
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwDBDemo", msg)
    }

    private fun clearLog() {
        tvLog.text = ""
        log("🗑️ 日志已清除")
    }

    private fun insertUser() {
        lifecycleScope.launch {
            log("🔄 开始插入用户...")
            val dao = db.userDao()
            val user = User(name = "用户-${System.currentTimeMillis() % 1000}", age = (20..60).random())
            val id = dao.insert(user)
            log("✅ 插入成功: $user, ID=$id")
        }
    }

    private fun batchInsert() {
        lifecycleScope.launch {
            log("🔄 开始批量插入...")
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "批量-$it", age = (20..30).random())
            }
            val ids = dao.insertAll(users)
            log("✅ 批量插入成功: ${ids.size}个用户, IDs=$ids")
        }
    }

    private fun queryUsers() {
        lifecycleScope.launch {
            log("🔄 开始查询所有用户...")
            val users = db.userDao().getAll()
            log("📋 用户总数: ${users.size}")
            users.forEach { log("  $it") }
        }
    }

    private fun queryById() {
        lifecycleScope.launch {
            log("🔄 开始根据ID查询...")
            val user = db.userDao().getById(1)
            if (user != null) {
                log("✅ 找到用户: $user")
            } else {
                log("❌ 未找到用户 (ID=1)")
            }
        }
    }

    private fun upsertUser() {
        lifecycleScope.launch {
            log("🔄 开始更新/插入用户...")
            val dao = db.userDao()
            val user = User(id = 1, name = "更新-${System.currentTimeMillis() % 1000}", age = 99)
            val id = dao.upsert(user)
            log("✅ 更新/插入成功: $user, 结果=$id")
        }
    }

    private fun countUsers() {
        lifecycleScope.launch {
            log("🔄 开始统计用户数...")
            val count = db.userDao().count()
            log("📊 用户总数: $count")
        }
    }

    private fun deleteUsers() {
        lifecycleScope.launch {
            log("🔄 开始删除所有用户...")
            db.userDao().deleteAll()
            log("✅ 所有用户已删除")
        }
    }

    private fun testDbResult() {
        lifecycleScope.launch {
            log("🔄 开始测试 DbResult...")
            val result = dbResultOf { db.userDao().getAll() }
            result.fold(
                onLoading = { log("⏳ 加载中...") },
                onSuccess = { log("✅ DbResult 成功: ${it.size}个用户") },
                onFailure = { log("❌ DbResult 失败: ${it.message}") }
            )
        }
    }

    private fun testWithTx() {
        lifecycleScope.launch {
            log("🔄 开始测试事务...")
            val result = db.safeTransaction {
                val dao = userDao()
                dao.insert(User(name = "事务1", age = 25))
                dao.insert(User(name = "事务2", age = 30))
                dao.getAll()
            }
            result.onSuccess { log("✅ 事务成功: ${it.size}个用户") }
            result.onFailure { log("❌ 事务失败: ${it.message}") }
        }
    }

    private fun testBatchExecute() {
        lifecycleScope.launch {
            log("🔄 开始测试批量执行...")
            val dao = db.userDao()
            val users = (1..5).map {
                User(name = "批量执行-$it", age = (20..30).random())
            }
            val result = db.batchExecute(users) { user ->
                dao.insert(user)
            }
            when (result) {
                is BatchResult.Skipped -> log("✅ 批量执行成功: 成功${result.successCount}个, 失败${result.failedCount}个")
                is BatchResult.AllOrNothing -> result.result.onSuccess { log("✅ 批量执行全部成功: $it") }
                    .onFailure { log("❌ 批量执行失败: ${it.message}") }
            }
        }
    }

    private fun testPagination() {
        lifecycleScope.launch {
            log("🔄 开始测试分页查询...")
            val pageSize = 3
            val page = 1
            val users = db.userDao().getPaged(pageSize, (page - 1) * pageSize)
            log("📋 第${page}页 (每页${pageSize}条): ${users.size}个用户")
            users.forEach { log("  $it") }
        }
    }

    private fun testComplexQuery() {
        lifecycleScope.launch {
            log("🔄 开始测试复杂查询...")
            val users = db.userDao().getUsersByAgeRange(25, 40)
            log("📋 25-40岁用户: ${users.size}个")
            users.forEach { log("  $it") }

            val sortedUsers = db.userDao().getUsersSortedByName()
            log("📋 按姓名排序: ${sortedUsers.size}个用户")
            sortedUsers.forEach { log("  $it") }
        }
    }

    private fun observeFlow() {
        lifecycleScope.launch {
            log("🔄 开始观察 Flow...")
            db.userDao().observeAll()
                .asDbResultWithLoading()
                .collect { result ->
                    result.fold(
                        onLoading = { log("[Flow] ⏳ 加载中...") },
                        onSuccess = { log("[Flow] ✅ 收到: ${it.size}个用户") },
                        onFailure = { log("[Flow] ❌ 错误: ${it.message}") }
                    )
                }
        }
    }

    private fun showDatabaseInfo() {
        lifecycleScope.launch {
            log("🔍 数据库信息:")
            log("  数据库版本: ${db.openHelper.readableDatabase.version}")
            log("  表名: user")
            log("  索引: user_name_index")
            log("  连接状态: 已连接")
        }
    }

    private fun releaseConnection() {
        DatabaseManager.release("demo.db")
        log("🔌 数据库连接已释放")
    }
}
