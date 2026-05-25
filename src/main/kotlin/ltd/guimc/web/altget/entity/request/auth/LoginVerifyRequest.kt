package ltd.guimc.web.altget.entity.request.auth

data class LoginVerifyRequest(
    val sessionId: String,
    val a: String,
    val m1: String
)