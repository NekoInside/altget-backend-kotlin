package ltd.guimc.web.altget.entity.response.user

import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import ltd.guimc.web.altget.enum.EnumUserRole

data class UserInfo(val id: Int, val name: String,
                    val email: String, val role: EnumUserRole,
                    val apiLimitLevel: EnumApiLimitLevel,
                    val registrationTime: String, val lastLoginIp: String,
                    val lastLoginGeo: String, val lastLoginTime: String,
                    val totalAltFetched: Int, val dailyAltFetched: Int,
                    val totalWebFetched: Int, val dailyWebFetched: Int,
                    val totalUserApiFetched: Int, val dailyUserApiFetched: Int,
                    val totalPaidApiFetched: Int, val dailyPaidApiFetched: Int)