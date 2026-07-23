package ltd.guimc.web.altget.component

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.net.InetAddress

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
        // Browser CORS preflight requests normally do not carry the auth token.
        // Let Spring's CORS handling finish the OPTIONS response before auth checks.
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            return true
        }

        resolveUserId(request)

        val realIp = resolveRealIp(request)
        request.setAttribute(REAL_IP_ATTRIBUTE, realIp)

        if (isAdminEndpoint(request)) {
            val userId = request.getAttribute(USER_ID_ATTRIBUTE) as? Int
            val status = when {
                userId == null -> 401
                userDetailsService.getById(userId)?.userRole != EnumUserRole.ADMIN -> 403
                else -> null
            }
            if (status != null) {
                response.status = status
                response.characterEncoding = Charsets.UTF_8.name()
                response.contentType = "application/json"
                response.writer.write(
                    if (status == 401) {
                        "{\"code\":401,\"message\":\"Unauthorized\",\"data\":null}"
                    } else {
                        "{\"code\":403,\"message\":\"Forbidden: Admin role required\",\"data\":null}"
                    },
                )
                return false
            }
        }

        return true
    }

    private fun isAdminEndpoint(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath ?: "")
        return path == "/api/admin" || path.startsWith("/api/admin/")
    }

    private fun resolveRealIp(request: HttpServletRequest): String {
        val sourceIp = request.remoteAddr
        val sourceAddress = sourceIp.toInetAddressOrNull()
        val forwardedIp = request.getHeader("X-Real-IP")
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (sourceAddress == null || !sourceAddress.isReservedSourceAddress()) {
            return sourceIp
        }

        return forwardedIp ?: sourceIp
    }

    private fun String.toInetAddressOrNull(): InetAddress? = try {
        InetAddress.getByName(this)
    } catch (_: Exception) {
        null
    }

    private fun InetAddress.isReservedSourceAddress(): Boolean {
        if (isAnyLocalAddress || isLoopbackAddress || isSiteLocalAddress || isLinkLocalAddress || isMulticastAddress) {
            return true
        }

        val addressBytes = address
        if (addressBytes.size == 4) {
            val firstOctet = addressBytes[0].toInt() and 0xFF
            val secondOctet = addressBytes[1].toInt() and 0xFF
            return firstOctet == 0 ||
                firstOctet == 10 ||
                (firstOctet == 100 && secondOctet in 64..127) ||
                (firstOctet == 169 && secondOctet == 254) ||
                (firstOctet == 172 && secondOctet in 16..31) ||
                (firstOctet == 192 && secondOctet == 0) ||
                (firstOctet == 192 && secondOctet == 168) ||
                (firstOctet == 198 && secondOctet in 18..19) ||
                firstOctet >= 224
        }

        return hostAddress.startsWith("fc", ignoreCase = true) ||
            hostAddress.startsWith("fd", ignoreCase = true) ||
            hostAddress == "::" ||
            hostAddress == "::1"
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
