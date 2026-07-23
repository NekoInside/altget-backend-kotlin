package ltd.guimc.web.altget.entity.response.admin

import ltd.guimc.web.altget.entity.response.user.AdminOperationLogResponse

data class AdminDashboardSummaryResponse(
    val totalUsers: Long,
    val verifiedUsers: Long,
    val availableTokens: Long,
    val paidRechargeOrders: Long,
    val recentOperations: List<AdminOperationLogResponse>,
)
