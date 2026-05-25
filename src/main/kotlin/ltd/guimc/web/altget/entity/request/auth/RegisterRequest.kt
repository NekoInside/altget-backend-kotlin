package ltd.guimc.web.altget.entity.request.auth

import ltd.guimc.web.altget.entity.request.CaptchaRequest

data class RegisterRequest(
    val username: String,
    val email: String,
    val salt: String,
    val verifier: String
) : CaptchaRequest()