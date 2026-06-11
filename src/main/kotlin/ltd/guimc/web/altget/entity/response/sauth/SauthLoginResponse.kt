package ltd.guimc.web.altget.entity.response.sauth

data class SauthLoginResponse(
    val success: Boolean,
    val message: String,
    val sauthJson: String? = null
)
