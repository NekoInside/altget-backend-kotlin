package ltd.guimc.web.altget.entity.response.alt

/**
 * Response DTO for a consumed (used) alt account.
 */
data class UsedAltResponse(
    val id: Long?,
    val username: String?,
    val password: String?,
    val channel: String?,
    val userId: Int?,
    val operationIp: String?,
    val fetchMethod: String,
    val fetchTime: String,
    val createdAt: String?
)
