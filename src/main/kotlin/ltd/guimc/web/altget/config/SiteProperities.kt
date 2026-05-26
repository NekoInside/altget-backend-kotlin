package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "site")
data class SiteProperities(
    val domain: String = "localhost:8080",
    val coinBuyUrl: String = "https://example.com/buy",
    val fakeAccountSalt: String = "default_salt_value",
    val srpSessionTtl: Int = 60,
    val githubClientId: String = "",
    val githubClientSecret: String = ""
)