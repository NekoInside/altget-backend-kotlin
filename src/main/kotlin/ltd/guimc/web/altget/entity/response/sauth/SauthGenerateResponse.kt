package ltd.guimc.web.altget.entity.response.sauth

data class SauthGenerateResponse(
    val username: String,
    val password: String,
    val sauth: String
)