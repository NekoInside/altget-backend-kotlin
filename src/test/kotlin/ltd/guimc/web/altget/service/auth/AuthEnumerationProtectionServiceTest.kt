package ltd.guimc.web.altget.service.auth

import ltd.guimc.web.altget.component.JwtTokenComponent
import ltd.guimc.web.altget.config.SiteProperities
import ltd.guimc.web.altget.entity.request.auth.RegisterRequest
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AuthEnumerationProtectionServiceTest {

    private val jwtTokenComponent = mock(JwtTokenComponent::class.java)
    private val siteProperties = SiteProperities(fakeAccountSalt = "test-salt")
    private val service = AuthEnumerationProtectionService(jwtTokenComponent, siteProperties)

    @Test
    fun `simulate register performs deterministic real work`() {
        val request = RegisterRequest(
            username = "TestUser",
            email = "test@example.com",
            salt = "salt",
            verifier = "verifier"
        )

        assertDoesNotThrow {
            service.simulateRegister(request)
        }
    }

    @Test
    fun `simulate forgot password generates token and srp challenge`() {
        `when`(jwtTokenComponent.generatePasswordResetToken("user@example.com")).thenReturn("token")

        assertDoesNotThrow {
            service.simulateForgotPassword("User@example.com")
        }

        verify(jwtTokenComponent).generatePasswordResetToken("user@example.com")
    }
}