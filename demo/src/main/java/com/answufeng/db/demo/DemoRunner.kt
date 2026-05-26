package com.answufeng.db.demo

/** MainActivity 实现，向各 [DemoSectionFragment] 提供执行入口。 */
fun interface DemoRunner {
    fun run(action: DemoAction)
}
