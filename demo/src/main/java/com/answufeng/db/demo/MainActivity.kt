package com.answufeng.db.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingSource
import com.answufeng.db.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), DemoRunner {

    private lateinit var db: AppDatabase
    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var tvStats: TextView

    private var flowJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dbName = "demo.db"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)
        tvStats = findViewById(R.id.tvStats)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.subtitle = getString(R.string.subtitle_main)

        db = DatabaseManager.getOrCreate<AppDatabase>(this, dbName) {
            fallbackToDestructiveMigration()
        }

        val pager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.pagerSections)
        pager.adapter = DemoPagerAdapter(this)
        pager.offscreenPageLimit = DemoSection.entries.size

        TabLayoutMediator(findViewById(R.id.tabSections), pager) { tab, position ->
            tab.text = getString(DemoSection.entries[position].titleRes)
        }.attach()

        refreshStats()
        logSection(DemoSection.CRUD, "数据库已打开: $dbName")

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCopyLog)
            .setOnClickListener { copyLog() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShareLog)
            .setOnClickListener { shareLog() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClearLog)
            .setOnClickListener { clearLog() }
    }

    override fun run(action: DemoAction) {
        when (action) {
            DemoAction.INSERT -> insertUser()
            DemoAction.BATCH_INSERT -> batchInsert()
            DemoAction.QUERY_ALL -> queryUsers()
            DemoAction.QUERY_BY_ID -> queryById()
            DemoAction.UPSERT -> upsertUser()
            DemoAction.COUNT -> countUsers()
            DemoAction.INSERT_OR_IGNORE -> insertOrIgnore()
            DemoAction.UPDATE -> updateUser()
            DemoAction.DELETE -> deleteUser()
            DemoAction.DELETE_ALL -> deleteUsers()
            DemoAction.DB_RESULT -> testDbResult()
            DemoAction.OBSERVE_FLOW -> observeFlow()
            DemoAction.TRANSACTION -> testWithTx()
            DemoAction.BATCH_EXECUTE -> testBatchExecute()
            DemoAction.PAGED_RESULT -> testPagedResult()
            DemoAction.PAGING_SOURCE -> testPagingSource()
            DemoAction.DEBUG_HELPER -> testDebugHelper()
            DemoAction.BACKUP -> testBackup()
            DemoAction.RESTORE -> testRestore()
            DemoAction.GET_OR_NULL -> testGetOrNull()
            DemoAction.DB_PATH -> showDbPath()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.demo_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> {
            refreshStats()
            true
        }
        R.id.action_demo_playbook -> {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_playbook_title)
                .setMessage(R.string.demo_playbook_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        flowJob?.cancel()
        DatabaseManager.release(dbName)
    }

    private fun logSection(section: DemoSection, msg: String) {
        val tag = getString(section.titleRes)
        log("[$tag] $msg")
    }

    private fun log(msg: String) {
        val timestamp = timeFormat.format(Date())
        tvLog.append("[$timestamp] $msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwDBDemo", msg)
    }

    private fun refreshStats() {
        val backup = backupFile()
        val dbLabel = if (backup.exists()) {
            "$dbName · ${getString(R.string.backup_ready)}"
        } else {
            dbName
        }
        val flowLabel = if (flowJob?.isActive == true) {
            getString(R.string.flow_on)
        } else {
            getString(R.string.flow_off)
        }
        tvStats.text = getString(R.string.stats_format, dbLabel, "…", flowLabel)
        lifecycleScope.launch {
            val countStr = runCatching { db.userDao().count().toString() }
                .getOrElse { "—" }
            tvStats.text = getString(R.string.stats_format, dbLabel, countStr, flowLabel)
        }
    }

    private fun updateFlowStats() {
        refreshStats()
    }

    private fun clearLog() {
        tvLog.text = ""
        log("日志已清空")
    }

    private fun copyLog() {
        val text = tvLog.text?.toString().orEmpty()
        if (text.isBlank()) {
            log("日志为空")
            return
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("aw-db-demo-log", text))
        log("已复制到剪贴板")
    }

    private fun shareLog() {
        val text = tvLog.text?.toString().orEmpty()
        if (text.isBlank()) {
            log("日志为空")
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "aw-db demo log")
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                getString(R.string.action_share_log)
            )
        )
    }

    private fun insertUser() {
        lifecycleScope.launch {
            logSection(DemoSection.CRUD, "插入用户…")
            val dao = db.userDao()
            val user = User(
                name = "用户-${System.currentTimeMillis() % 1000}",
                age = (20..60).random(),
                tags = listOf("demo")
            )
            val id = dao.insert(user)
            log("insert → id=$id, $user")
            refreshStats()
        }
    }

    private fun batchInsert() {
        lifecycleScope.launch {
            logSection(DemoSection.CRUD, "insertAll ×5…")
            val users = (1..5).map {
                User(name = "批量-$it", age = (20..30).random(), tags = listOf("batch"))
            }
            val ids = db.userDao().insertAll(users)
            log("insertAll → ${ids.size} 条, ids=$ids")
            refreshStats()
        }
    }

    private fun queryUsers() {
        lifecycleScope.launch {
            logSection(DemoSection.CRUD, "getAll…")
            val users = db.userDao().getAll()
            log("共 ${users.size} 条")
            users.take(8).forEach { log("  $it") }
            if (users.size > 8) log("  …")
            refreshStats()
        }
    }

    private fun queryById() {
        val inputLayout = TextInputLayout(this).apply { hint = "用户 ID" }
        val editText = TextInputEditText(this).apply { setText("1") }
        inputLayout.addView(editText)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.demo_query_id_title)
            .setView(inputLayout)
            .setPositiveButton(R.string.action_run) { _, _ ->
                val id = editText.text?.toString()?.trim()?.toLongOrNull()
                if (id == null || id <= 0) {
                    log("无效 ID")
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    logSection(DemoSection.CRUD, "getById($id)…")
                    val user = db.userDao().getById(id)
                    log(if (user != null) "找到: $user" else "未找到 id=$id")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun upsertUser() {
        lifecycleScope.launch {
            logSection(DemoSection.CRUD, "upsert id=1…")
            val user = User(id = 1, name = "Upsert-${System.currentTimeMillis() % 1000}", age = 99)
            val id = db.userDao().upsert(user)
            log("upsert → $id, $user")
            refreshStats()
        }
    }

    private fun countUsers() {
        lifecycleScope.launch {
            val count = db.userDao().count()
            logSection(DemoSection.CRUD, "count → $count")
            refreshStats()
        }
    }

    private fun deleteUsers() {
        lifecycleScope.launch {
            logSection(DemoSection.CRUD, "deleteAll…")
            db.userDao().deleteAll()
            log("表已清空")
            refreshStats()
        }
    }

    private fun insertOrIgnore() {
        lifecycleScope.launch {
            logSection(DemoSection.CRUD, "insertOrIgnore 冲突测试…")
            val dao = db.userDao()
            val id1 = dao.insertOrIgnore(User(id = 1, name = "A", age = 25))
            val id2 = dao.insertOrIgnore(User(id = 1, name = "B", age = 30))
            log("首次 id=$id1, 重复 id=$id2 (期望 -1)")
        }
    }

    private fun updateUser() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val user = dao.getById(1)
            if (user == null) {
                log("无 id=1，请先插入")
                return@launch
            }
            val updated = user.copy(name = "更新-${System.currentTimeMillis() % 1000}")
            val rows = dao.update(updated)
            logSection(DemoSection.CRUD, "update → rows=$rows")
            refreshStats()
        }
    }

    private fun deleteUser() {
        lifecycleScope.launch {
            val dao = db.userDao()
            val user = dao.getById(1)
            if (user == null) {
                log("无 id=1")
                return@launch
            }
            val rows = dao.delete(user)
            logSection(DemoSection.CRUD, "delete → rows=$rows")
            refreshStats()
        }
    }

    private fun testDbResult() {
        lifecycleScope.launch {
            logSection(DemoSection.RESULT, "dbResultOf…")
            dbResultOf { db.userDao().getAll() }.fold(
                onLoading = { log("Loading") },
                onSuccess = { log("Success: ${it.size} 条") },
                onFailure = { log("Failure: ${it.message}") }
            )
            val failed: DbResult<String> = DbResult.Failure(RuntimeException("demo"))
            val recovered = failed.getOrElse { "默认值" }
            log("getOrElse → $recovered")
        }
    }

    private fun testWithTx() {
        lifecycleScope.launch {
            logSection(DemoSection.TRANSACTION, "safeTransaction…")
            db.safeTransaction {
                userDao().insert(User(name = "Tx-1", age = 25, tags = listOf("tx")))
                userDao().insert(User(name = "Tx-2", age = 30, tags = listOf("tx")))
                userDao().getAll()
            }.onSuccess { log("成功: ${it.size} 条") }
                .onFailure { log("失败: ${it.message}") }
            refreshStats()
        }
    }

    private fun testBatchExecute() {
        lifecycleScope.launch {
            logSection(DemoSection.TRANSACTION, "batchExecute SKIP…")
            val users = (1..5).map { User(name = "Batch-$it", age = 20 + it) }
            when (val r = db.batchExecute(users) { db.userDao().insert(it) }) {
                is BatchResult.Skipped ->
                    log("成功 ${r.successCount}, 失败 ${r.failedCount}")
                is BatchResult.AllOrNothing -> log("FAIL_FAST 分支")
            }
            refreshStats()
        }
    }

    private fun observeFlow() {
        if (flowJob?.isActive == true) {
            flowJob?.cancel()
            flowJob = null
            updateFlowStats()
            logSection(DemoSection.RESULT, "Flow 已停止")
            return
        }
        flowJob = lifecycleScope.launch {
            updateFlowStats()
            logSection(DemoSection.RESULT, "asDbResultWithLoading 收集中…")
            db.userDao().observeAll()
                .asDbResultWithLoading()
                .collect { r ->
                    r.fold(
                        onLoading = { log("[Flow] Loading") },
                        onSuccess = { log("[Flow] ${it.size} 条") },
                        onFailure = { log("[Flow] ${it.message}") }
                    )
                }
        }
    }

    private fun testDebugHelper() {
        lifecycleScope.launch {
            logSection(DemoSection.OPS, "DbDebugHelper…")
            db.tableList().forEach { t ->
                log("$t: ${db.rowCount(t)} 行")
            }
            db.tableSchema("User").forEach { col ->
                log("  ${col.name} ${col.type}")
            }
            log("ref=${DatabaseManager.getReferenceCount(dbName)}, managed=${DatabaseManager.isManaged(dbName)}")
        }
    }

    private fun testPagingSource() {
        lifecycleScope.launch {
            logSection(DemoSection.PAGING, "PagingSource Refresh…")
            val source = db.userDao().pagingSource()
            val params = PagingSource.LoadParams.Refresh<Int>(
                key = null,
                loadSize = 5,
                placeholdersEnabled = false
            )
            when (val page = source.load(params)) {
                is PagingSource.LoadResult.Page -> {
                    log("首屏 ${page.data.size} 条")
                    page.data.forEach { log("  $it") }
                }
                is PagingSource.LoadResult.Error -> log("Error: ${page.throwable.message}")
                is PagingSource.LoadResult.Invalid -> log("Invalid")
            }
        }
    }

    private fun testPagedResult() {
        lifecycleScope.launch {
            logSection(DemoSection.PAGING, "toPagedResult…")
            val dao = db.userDao()
            val pageSize = 3
            val items = dao.getPage(pageSize, 0)
            val result = items.toPagedResult(0, pageSize, dao.count())
            log("page=${result.page}, items=${result.items.size}, total=${result.total}, hasMore=${result.hasMore}")
            refreshStats()
        }
    }

    private fun testBackup() {
        lifecycleScope.launch {
            logSection(DemoSection.OPS, "backupTo…")
            runCatching {
                val file = backupFile()
                db.backupTo(file)
                log("→ ${file.absolutePath} (${file.length()} B)")
                refreshStats()
            }.onFailure { log("失败: ${it.message}") }
        }
    }

    private fun testRestore() {
        lifecycleScope.launch {
            val file = backupFile()
            if (!file.exists()) {
                log("无备份: ${file.absolutePath}")
                return@launch
            }
            logSection(DemoSection.OPS, "restore…")
            runCatching {
                db = DbBackupHelper.restore<AppDatabase>(this@MainActivity, dbName, file) {
                    fallbackToDestructiveMigration()
                }
            }.onSuccess {
                log("恢复成功，请丢弃旧 RoomDatabase 引用")
                refreshStats()
            }.onFailure { log("失败: ${it.message}") }
        }
    }

    private fun testGetOrNull() {
        logSection(DemoSection.OPS, "DatabaseManager…")
        log("getOrNull('$dbName')=${DatabaseManager.getOrNull<AppDatabase>(dbName) != null}")
        log("getOrNull('x.db')=${DatabaseManager.getOrNull<AppDatabase>("x.db") != null}")
        log("isManaged($dbName)=${DatabaseManager.isManaged(dbName)}")
    }

    private fun showDbPath() {
        val path = getDatabasePath(dbName).absolutePath
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.demo_db_path_title)
            .setMessage(path)
            .setPositiveButton(R.string.action_copy_log) { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("db-path", path))
                log("路径已复制")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun backupFile(): File {
        val dir = File(getExternalFilesDir(null), "backup")
        dir.mkdirs()
        return File(dir, "demo_backup.db")
    }
}
