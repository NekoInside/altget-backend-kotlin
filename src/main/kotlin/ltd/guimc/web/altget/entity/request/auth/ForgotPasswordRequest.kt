package ltd.guimc.web.altget.entity.request.auth

import ltd.guimc.web.altget.entity.request.CaptchaRequest

data class ForgotPasswordRequest(
    val email: String
) : CaptchaRequest()
