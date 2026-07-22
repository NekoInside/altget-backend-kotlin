package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cookie-convert")
data class CookieConvertProperties(
    val upstreamUrl: String = "",
    val timeoutMillis: Int = 10_000,
)
