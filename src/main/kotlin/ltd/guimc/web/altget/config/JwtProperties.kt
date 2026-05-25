package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * JWT configuration properties, bound from `jwt.*` in application.yaml.
 */
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    /** Secret key used to sign and verify JWT tokens (HMAC-SHA) */
    val secret: String = "",

    /** Token issuer (iss claim), typically the application name or domain */
    val issuer: String = "altget",

    /** Access token expiration duration (e.g. "7d") */
    val accessTokenExpiration: Duration = Duration.ofDays(7),

    /** Refresh token expiration duration (e.g. "30d") */
    val registerVerifyTokenExpiration: Duration = Duration.ofMinutes(10)
)
