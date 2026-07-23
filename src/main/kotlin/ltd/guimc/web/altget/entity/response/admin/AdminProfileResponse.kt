package ltd.guimc.web.altget.entity.response.admin

/** Public, non-sensitive profile information for the currently authenticated administrator. */
data class AdminProfileResponse(
    val id: Long,
    val username: String,
    val displayName: String?,
    val email: String?,
    val role: String,
    val avatarUrl: String?,
)
