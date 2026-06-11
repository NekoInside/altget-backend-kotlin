package ltd.guimc.web.altget.entity.response.user

import ltd.guimc.web.altget.enum.EnumUserOperation

data class AdminOperationLogResponse(
    val operationId: String,
    val operationTime: String,
    val userId: Int?,
    val username: String?,
    val operation: EnumUserOperation,
    val description: String,
    val ip: String?,
    val geoip: String?
)
