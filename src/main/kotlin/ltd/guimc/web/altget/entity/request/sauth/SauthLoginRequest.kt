package ltd.guimc.web.altget.entity.request.sauth

data class SauthLoginRequest(
    val username: String,
    val password: String,
    val proxy: String? = null
)
