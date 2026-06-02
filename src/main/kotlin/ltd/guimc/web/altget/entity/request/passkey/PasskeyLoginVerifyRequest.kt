package ltd.guimc.web.altget.entity.request.passkey

/**
 * Request body for POST /api/passkey/login/verify
 */
data class PasskeyLoginVerifyRequest(
    val challengeId: String,
    val credential: String
)
