package ltd.guimc.web.altget.component

import ltd.guimc.web.altget.service.user.UserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.mockito.Mockito.mock

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
}