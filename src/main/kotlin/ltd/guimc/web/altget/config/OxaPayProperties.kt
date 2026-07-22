package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oxapay")
data class OxaPayProperties(
    val merchantApiKey: String = "",
    val apiBaseUrl: String = "https://api.oxapay.com/v1",
    val callbackUrl: String = "",
    val returnUrl: String = "",
    val invoiceLifetimeMinutes: Int = 60,
    val sandbox: Boolean = false,
    val timeoutMillis: Int = 10_000,
)
