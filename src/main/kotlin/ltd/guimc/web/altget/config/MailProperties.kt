package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Mail configuration properties, bound from `mail.*` in application.yaml.
 */
@ConfigurationProperties(prefix = "mail")
data class MailProperties(
    /** Sender "from" address used for all outgoing emails */
    val from: String = "noreply@example.com",

    /** Base domain used in email template links (e.g. "example.com") */
    val domain: String = "example.com",

    /** Default expiration time in minutes for activation/reset links */
    val defaultExpirationMinutes: Long = 30
)
