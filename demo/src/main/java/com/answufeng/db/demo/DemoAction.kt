package com.answufeng.db.demo

/**
 * Demo 可执行操作；按 aw-db 能力分组，与 [DemoSection] 一一对应。
 */
enum class DemoAction {
    INSERT,
    BATCH_INSERT,
    QUERY_ALL,
    QUERY_BY_ID,
    UPSERT,
    COUNT,
    INSERT_OR_IGNORE,
    UPDATE,
    DELETE,
    DELETE_ALL,
    DB_RESULT,
    OBSERVE_FLOW,
    TRANSACTION,
    BATCH_EXECUTE,
    PAGED_RESULT,
    PAGING_SOURCE,
    DEBUG_HELPER,
    BACKUP,
    RESTORE,
    GET_OR_NULL,
    DB_PATH,
}

enum class DemoSection(
    val titleRes: Int,
    val descRes: Int
) {
    CRUD(R.string.tab_crud, R.string.tab_crud_desc),
    RESULT(R.string.tab_result, R.string.tab_result_desc),
    TRANSACTION(R.string.tab_transaction, R.string.tab_transaction_desc),
    PAGING(R.string.tab_paging, R.string.tab_paging_desc),
    OPS(R.string.tab_ops, R.string.tab_ops_desc),
}

data class DemoActionItem(
    val action: DemoAction,
    val titleRes: Int,
    val subtitleRes: Int,
    val apiTagRes: Int,
)

object DemoCatalog {
    fun itemsFor(section: DemoSection): List<DemoActionItem> = when (section) {
        DemoSection.CRUD -> listOf(
            item(DemoAction.INSERT, R.string.demo_insert_title, R.string.demo_insert_sub, R.string.api_base_dao_insert),
            item(DemoAction.BATCH_INSERT, R.string.demo_batch_insert_title, R.string.demo_batch_insert_sub, R.string.api_base_dao_insert_all),
            item(DemoAction.QUERY_ALL, R.string.demo_query_all_title, R.string.demo_query_all_sub, R.string.api_dao_query),
            item(DemoAction.QUERY_BY_ID, R.string.demo_query_id_title, R.string.demo_query_id_sub, R.string.api_dao_query),
            item(DemoAction.UPSERT, R.string.demo_upsert_title, R.string.demo_upsert_sub, R.string.api_base_dao_upsert),
            item(DemoAction.INSERT_OR_IGNORE, R.string.demo_ignore_title, R.string.demo_ignore_sub, R.string.api_base_dao_ignore),
            item(DemoAction.UPDATE, R.string.demo_update_title, R.string.demo_update_sub, R.string.api_base_dao_update),
            item(DemoAction.DELETE, R.string.demo_delete_title, R.string.demo_delete_sub, R.string.api_base_dao_delete),
            item(DemoAction.COUNT, R.string.demo_count_title, R.string.demo_count_sub, R.string.api_dao_query),
            item(DemoAction.DELETE_ALL, R.string.demo_delete_all_title, R.string.demo_delete_all_sub, R.string.api_dao_query),
        )
        DemoSection.RESULT -> listOf(
            item(DemoAction.DB_RESULT, R.string.demo_db_result_title, R.string.demo_db_result_sub, R.string.api_db_result_of),
            item(DemoAction.OBSERVE_FLOW, R.string.demo_flow_title, R.string.demo_flow_sub, R.string.api_as_db_result_loading),
        )
        DemoSection.TRANSACTION -> listOf(
            item(DemoAction.TRANSACTION, R.string.demo_tx_title, R.string.demo_tx_sub, R.string.api_safe_transaction),
            item(DemoAction.BATCH_EXECUTE, R.string.demo_batch_exec_title, R.string.demo_batch_exec_sub, R.string.api_batch_execute),
        )
        DemoSection.PAGING -> listOf(
            item(DemoAction.PAGING_SOURCE, R.string.demo_paging_title, R.string.demo_paging_sub, R.string.api_as_paging_flow),
            item(DemoAction.PAGED_RESULT, R.string.demo_paged_title, R.string.demo_paged_sub, R.string.api_paged_result),
        )
        DemoSection.OPS -> listOf(
            item(DemoAction.DEBUG_HELPER, R.string.demo_debug_title, R.string.demo_debug_sub, R.string.api_debug_helper),
            item(DemoAction.BACKUP, R.string.demo_backup_title, R.string.demo_backup_sub, R.string.api_backup),
            item(DemoAction.RESTORE, R.string.demo_restore_title, R.string.demo_restore_sub, R.string.api_restore),
            item(DemoAction.GET_OR_NULL, R.string.demo_get_or_null_title, R.string.demo_get_or_null_sub, R.string.api_database_manager),
            item(DemoAction.DB_PATH, R.string.demo_db_path_title, R.string.demo_db_path_sub, R.string.api_database_manager),
        )
    }

    private fun item(action: DemoAction, title: Int, subtitle: Int, api: Int) =
        DemoActionItem(action, title, subtitle, api)
}
