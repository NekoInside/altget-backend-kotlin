package ltd.guimc.web.altget.component

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RequestInterceptor(private val jwtTokenComponent: JwtTokenComponent,
                         private val userDetailsService: UserDetailsService
) : HandlerInterceptor {

    companion object {
        const val USER_ID_ATTRIBUTE = "USER_ID_ATTRIBUTE"
        const val REAL_IP_ATTRIBUTE = "REAL_IP_ATTRIBUTE"
    }

    private val log = LoggerFactory.getLogger(RequestInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        resolveUserId(request)

        val realIp = request.getHeader("X-Real-IP")?.takeIf { it.isNotBlank() } ?: request.remoteAddr
        request.setAttribute(REAL_IP_ATTRIBUTE, realIp)

        return true
    }

    fun resolveUserId(request: HttpServletRequest) {
        val token = request.getHeader("X-Ciallo-Auth")
        if (token.isNullOrBlank()) return
        val userId = jwtTokenComponent.getUserIdFromToken(token) ?: return
        val userDetails = userDetailsService.getById(userId) ?: return
        if (userDetails.userRole == EnumUserRole.BANNED) return
        request.setAttribute(USER_ID_ATTRIBUTE, userDetails.userId)
    }
}