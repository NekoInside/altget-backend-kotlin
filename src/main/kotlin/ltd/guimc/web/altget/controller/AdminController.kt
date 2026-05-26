package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import ltd.guimc.web.altget.enum.EnumUserRole
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/api/admin")
class AdminController {
    @GetMapping("/users")
    fun listUsers(@CurrentUserId userId: Int?, size: Int, page: Int): ResponseBase<List<UserInfo>> {
        // TODO
        return ResponseBase(emptyList())
    }

    data class UserInfo(val id: Int, val name: String,
                        val email: String, val role: EnumUserRole,
                        val apiLimitLevel: EnumApiLimitLevel,
                        val registrationTime: String, val lastLoginIp: String,
                        val lastLoginGeo: String, val lastLoginTime: String,
                        val totalAltFetched: Int, val dailyAltFetched: Int,
                        val totalWebFetched: Int, val dailyWebFetched: Int,
                        val totalUserApiFetched: Int, val dailyUserApiFetched: Int,
                        val totalPaidApiFetched: Int, val dailyPaidApiFetched: Int)
}