package ltd.guimc.web.altget.entity.request.auth

data class ResetPasswordRequest(
    val token: String,
    val salt: String,
    val verifier: String
)
