package ltd.guimc.web.altget.component

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RequestInterceptor(private val jwtTokenComponent: JwtTokenComponent) : HandlerInterceptor {

    companion object {
        const val USER_ID_ATTRIBUTE = "USER_ID_ATTRIBUTE"
        const val REAL_IP_ATTRIBUTE = "REAL_IP_ATTRIBUTE"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val token = request.getHeader("X-Ciallo-Auth")

        if (!token.isNullOrBlank()) {
            val userId = jwtTokenComponent.getUserIdFromToken(token)
            if (userId != null) {
                request.setAttribute(USER_ID_ATTRIBUTE, userId)
            }
        }

        // we're currently not using CDN, so we can directly get the real IP from the request
        val realIp = request.remoteAddr
        request.setAttribute(REAL_IP_ATTRIBUTE, realIp)

        return true
    }
}