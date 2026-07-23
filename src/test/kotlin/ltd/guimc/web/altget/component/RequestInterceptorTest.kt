package ltd.guimc.web.altget.component

import ltd.guimc.web.altget.service.user.UserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.enum.EnumUserRole
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class RequestInterceptorTest {

    private val jwtTokenComponent = mock(JwtTokenComponent::class.java)
    private val userDetailsService = mock(UserDetailsService::class.java)
    private val interceptor = RequestInterceptor(jwtTokenComponent, userDetailsService)

    @Test
    fun `uses forwarded ip when request comes from private proxy address`() {
        val request = MockHttpServletRequest().apply {
            remoteAddr = "192.168.1.10"
            addHeader("X-Real-IP", "8.8.8.8")
        }

        interceptor.preHandle(request, MockHttpServletResponse(), Any())

        assertEquals("8.8.8.8", request.getAttribute(RequestInterceptor.REAL_IP_ATTRIBUTE))
    }

    @Test
    fun `ignores forwarded ip when request source is public`() {
        val request = MockHttpServletRequest().apply {
            remoteAddr = "8.8.4.4"
            addHeader("X-Real-IP", "1.1.1.1")
        }

        interceptor.preHandle(request, MockHttpServletResponse(), Any())

        assertEquals("8.8.4.4", request.getAttribute(RequestInterceptor.REAL_IP_ATTRIBUTE))
    }

    @Test
    fun `returns http 401 for an unauthenticated admin request`() {
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/admin/me"
        }
        val response = MockHttpServletResponse()

        assertFalse(interceptor.preHandle(request, response, Any()))
        assertEquals(401, response.status)
    }

    @Test
    fun `returns http 403 for a non-admin admin request`() {
        `when`(jwtTokenComponent.getUserIdFromToken("token")).thenReturn(7)
        `when`(userDetailsService.getById(7)).thenReturn(UserDetails().apply {
            userId = 7
            userRole = EnumUserRole.VERIFY
        })
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/admin/me"
            addHeader("X-Ciallo-Auth", "token")
        }
        val response = MockHttpServletResponse()

        assertFalse(interceptor.preHandle(request, response, Any()))
        assertEquals(403, response.status)
    }

    @Test
    fun `allows cors preflight without authentication`() {
        val request = MockHttpServletRequest().apply {
            method = "OPTIONS"
            requestURI = "/api/admin/me"
            addHeader("Origin", "http://localhost:3000")
            addHeader("Access-Control-Request-Method", "GET")
            addHeader("Access-Control-Request-Headers", "X-Ciallo-Auth")
        }

        assertTrue(interceptor.preHandle(request, MockHttpServletResponse(), Any()))
    }
}
