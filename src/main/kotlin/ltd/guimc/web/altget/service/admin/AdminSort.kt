package ltd.guimc.web.altget.service.admin

import ltd.guimc.web.altget.component.AdminBadRequestException

enum class AdminList {
    USERS,
    TOKENS,
    OPERATIONS,
    USED_ALTS,
    OXAPAY_RECHARGES,
}

enum class SortDirection {
    ASC,
    DESC,
}

data class AdminSortSpec(
    val column: String,
    val ascending: Boolean,
    val tieBreakerColumn: String,
)

/**
 * Maps public camelCase sort names to fixed database columns.
 * Never pass a request parameter directly to QueryWrapper.orderBy*.
 */
object AdminSort {
    private val fields = mapOf(
        AdminList.USERS to mapOf(
            "id" to "user_id",
            "name" to "username",
            "email" to "email",
            "role" to "user_role",
        ),
        AdminList.TOKENS to mapOf(
            "id" to "id",
            "coinAmount" to "coin_amount",
            "createdBy" to "created_by",
            "redeemedBy" to "redeemed_by",
            "isUsed" to "is_used",
            "createdAt" to "created_at",
        ),
        AdminList.OPERATIONS to mapOf(
            "operationId" to "operation_id",
            "userId" to "user_id",
            "operation" to "operation",
            "ip" to "ip",
            "operationTime" to "operation_time",
        ),
        AdminList.USED_ALTS to mapOf(
            "id" to "id",
            "username" to "username",
            "channel" to "channel",
            "fetchMethod" to "fetch_method",
            "userId" to "user_id",
            "operationIp" to "operation_ip",
            "fetchTime" to "fetch_time",
        ),
        AdminList.OXAPAY_RECHARGES to mapOf(
            "orderId" to "id",
            "userId" to "user_id",
            "usdAmount" to "usd_amount",
            "coinAmount" to "coin_amount",
            "status" to "status",
            "createdAt" to "created_at",
        ),
    )

    private val defaults = mapOf(
        AdminList.USERS to "id",
        AdminList.TOKENS to "createdAt",
        AdminList.OPERATIONS to "operationTime",
        AdminList.USED_ALTS to "fetchTime",
        AdminList.OXAPAY_RECHARGES to "createdAt",
    )

    fun resolve(list: AdminList, sortBy: String?, sortDirection: String?): AdminSortSpec {
        val direction = when (sortDirection?.lowercase()) {
            null, "desc" -> SortDirection.DESC
            "asc" -> SortDirection.ASC
            else -> throw AdminBadRequestException("sortDirection must be asc or desc")
        }
        val publicField = sortBy?.trim()?.takeIf { it.isNotEmpty() } ?: defaults.getValue(list)
        val column = fields.getValue(list)[publicField]
            ?: throw AdminBadRequestException(
                "Invalid sortBy '$publicField' for ${list.name.lowercase().replace('_', '-')}; " +
                    "allowed values: ${fields.getValue(list).keys.joinToString(", ")}",
            )
        return AdminSortSpec(
            column = column,
            ascending = direction == SortDirection.ASC,
            tieBreakerColumn = when (list) {
                AdminList.USERS -> "user_id"
                AdminList.TOKENS -> "id"
                AdminList.OPERATIONS -> "operation_id"
                AdminList.USED_ALTS -> "id"
                AdminList.OXAPAY_RECHARGES -> "id"
            },
        )
    }
}
