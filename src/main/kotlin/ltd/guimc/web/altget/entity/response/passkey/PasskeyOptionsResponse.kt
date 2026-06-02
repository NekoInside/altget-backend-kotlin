package ltd.guimc.web.altget.entity.response.passkey

/**
 * Response for passkey register/login options endpoints.
 */
data class PasskeyOptionsResponse(
    val challengeId: String,
    val options: Any?
)
