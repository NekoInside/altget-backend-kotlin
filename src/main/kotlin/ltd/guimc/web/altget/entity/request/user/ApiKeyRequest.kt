package ltd.guimc.web.altget.entity.request.user

import ltd.guimc.web.altget.entity.request.CaptchaRequest

data class ApiKeyRequest(
    val taskId: String,
    val result: String
) : CaptchaRequest()
