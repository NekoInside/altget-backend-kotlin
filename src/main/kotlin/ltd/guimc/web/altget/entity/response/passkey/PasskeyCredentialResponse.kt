package ltd.guimc.web.altget.entity.response.passkey

/**
 * Response item for GET /api/passkey/list
 */
data class PasskeyCredentialResponse(
    val id: Int,
    val name: String,
    val createdAt: String
)
