package ltd.guimc.web.altget.entity.response.user

import ltd.guimc.web.altget.enum.EnumUserRole

data class SimpleUserInfo(
    val name: String, val id: Int, val role: EnumUserRole, val email: String
)