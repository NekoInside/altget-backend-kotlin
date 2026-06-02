package ltd.guimc.web.altget.entity.request.passkey

/**
 * Request body for POST /api/passkey/register/verify
 */
data class PasskeyRegisterVerifyRequest(
    val challengeId: String,
    val credential: String,
    val name: String? = "My Passkey"
)
