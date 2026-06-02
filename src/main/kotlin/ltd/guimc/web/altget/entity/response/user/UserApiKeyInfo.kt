package ltd.guimc.web.altget.entity.response.user

import ltd.guimc.web.altget.enum.EnumApiLimitLevel

data class UserApiKeyInfo(
    val apiKey: String,
    val limitLevel: EnumApiLimitLevel
)
