package ltd.guimc.web.altget.entity.response.user

import ltd.guimc.web.altget.enum.EnumUserOperation

data class UserOperationLogResponse(
    val operationTime: String,
    val operation: EnumUserOperation,
    val description: String
)
