package ltd.guimc.web.altget.entity.response.auth

data class LoginChallengeResponse(
    val challengeId: String,
    val salt: String,
    val serverPublicKey: String
)