package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Optional Resend (https://resend.com) email delivery configuration, bound from `resend.*`.
 *
 * When [apiKey] is blank or a template's [ResendTemplate.id] is blank, email sending for
 * that template falls back to the existing SMTP/Thymeleaf path in [ltd.guimc.web.altget.component.EmailComponent].
 * No change to behavior occurs unless a valid API key and template id are supplied.
 *
 * Template keys ([templates] map) use the same names as the local Thymeleaf templates
 * (e.g. `activation`, `password-reset`).
 */
@ConfigurationProperties(prefix = "resend")
data class ResendProperties(
    /** Resend API key (`re_...`). When blank, Resend is disabled entirely. */
    val apiKey: String = "",

    /** Resend API base URL. */
    val apiBaseUrl: String = "https://api.resend.com",

    /** Sender "from" address for Resend. Falls back to [MailProperties.from] when blank. */
    val from: String = "",

    /** Resend templates keyed by local template name (e.g. `activation`, `password-reset`). */
    val templates: Map<String, ResendTemplate> = emptyMap()
) {
    /**
     * @param id the Resend template id (e.g. `tmpl_xxx`); blank disables Resend for this template.
     * @param variables mapping of internal variable names (as used by [ltd.guimc.web.altget.component.EmailComponent],
     *                  e.g. `username`, `activationUrl`) to the corresponding Resend template variable names.
     *                  Internal variables without a mapping are sent under their own name.
     */
    data class ResendTemplate(
        val id: String = "",
        val variables: Map<String, String> = emptyMap()
    )

    /** Whether Resend is available at all (requires an API key). */
    val isEnabled: Boolean get() = apiKey.isNotBlank()
}
