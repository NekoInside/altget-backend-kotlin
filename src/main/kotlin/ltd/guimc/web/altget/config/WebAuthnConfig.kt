package ltd.guimc.web.altget.config

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebAuthnConfig(
    private val webauthnProperties: WebAuthnProperties,
    private val passkeyCredentialRepository: PasskeyCredentialRepository
) {

    @Bean
    fun relyingParty(siteProperities: SiteProperities): RelyingParty {
        val rpIdentity = RelyingPartyIdentity.builder()
            .id(webauthnProperties.rpId)
            .name(webauthnProperties.rpName)
            .build()

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(passkeyCredentialRepository)
            .origins(setOf("https://${siteProperities.domain}"))
            .build()
    }
}

@ConfigurationProperties(prefix = "webauthn")
data class WebAuthnProperties(
    val rpId: String = "localhost",
    val rpName: String = "Altget",
)
