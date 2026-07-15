package ltd.guimc.web.altget.component

import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import ltd.guimc.web.altget.config.MailProperties
import ltd.guimc.web.altget.config.ResendProperties
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

/**
 * Component responsible for sending templated emails.
 *
 * Emails are sent via one of two channels, selected per template at runtime:
 *  - **Resend** (https://resend.com) using a pre-configured remote template, when a Resend
 *    API key and a template id for the given [templateName] are configured.
 *  - **SMTP** via Spring's [JavaMailSender] with HTML rendered locally by Thymeleaf, otherwise.
 *
 * Local Thymeleaf templates are resolved from `src/main/resources/templates/email/`.
 * Available templates: [activation], [password-reset].
 */
@Component
class EmailComponent(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,
    private val mailProperties: MailProperties,
    private val resendProperties: ResendProperties
) {

    private val log = LoggerFactory.getLogger(EmailComponent::class.java)

    companion object {
        private const val TEMPLATE_PATH_PREFIX = "email/"
    }

    /**
     * Send an account-activation email.
     *
     * @param to       recipient email address
     * @param username display name used in the greeting
     * @param activationUrl the full activation link for the user to click
     * @param expirationMinutes how long the link remains valid (defaults to [MailProperties.defaultExpirationMinutes])
     */
    fun sendActivationEmail(
        to: String,
        username: String,
        activationUrl: String,
        expirationMinutes: Long = mailProperties.defaultExpirationMinutes
    ) {
        val variables = mapOf(
            "username" to username,
            "activationUrl" to activationUrl,
            "expirationMinutes" to expirationMinutes,
            "domain" to mailProperties.domain
        )
        sendTemplateEmail(to, "账户激活确认 - 凌清阁", "activation", variables)
    }

    /**
     * Send a password-reset email.
     *
     * @param to       recipient email address
     * @param username display name used in the greeting
     * @param resetUrl the full password-reset link for the user to click
     * @param expirationMinutes how long the link remains valid (defaults to [MailProperties.defaultExpirationMinutes])
     */
    fun sendPasswordResetEmail(
        to: String,
        username: String,
        resetUrl: String,
        expirationMinutes: Long = mailProperties.defaultExpirationMinutes
    ) {
        val variables = mapOf(
            "username" to username,
            "resetUrl" to resetUrl,
            "expirationMinutes" to expirationMinutes,
            "domain" to mailProperties.domain
        )
        sendTemplateEmail(to, "密码重置请求 - 凌清阁", "password-reset", variables)
    }

    /**
     * Generic method to send a templated email.
     *
     * Dispatches to Resend when configured for [templateName]; otherwise renders the local
     * Thymeleaf template and sends via SMTP.
     *
     * @param to          recipient email address
     * @param subject     email subject line
     * @param templateName template name without the `.html` extension (relative to `email/`);
     *                     also used as the key into [ResendProperties.templates]
     * @param variables   template variables keyed by internal name
     */
    fun sendTemplateEmail(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, Any>
    ) {
        val resendTemplate = resendProperties.templates[templateName]
        if (resendProperties.isEnabled && resendTemplate != null && resendTemplate.id.isNotBlank()) {
            sendViaResend(to, subject, templateName, resendTemplate, variables)
        } else {
            sendViaSmtp(to, subject, templateName, variables)
        }
    }

    /**
     * Render the local Thymeleaf template and send via SMTP.
     */
    private fun sendViaSmtp(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, Any>
    ) {
        try {
            val context = Context().apply { variables.forEach { (k, v) -> setVariable(k, v) } }
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom(mailProperties.from)
            helper.setTo(to)
            helper.setSubject(subject)

            val htmlContent = templateEngine.process("$TEMPLATE_PATH_PREFIX$templateName", context)
            helper.setText(htmlContent, true)

            mailSender.send(mimeMessage)
            log.info("Email sent successfully: type={}, to={}, channel=smtp", templateName, to)
        } catch (e: MessagingException) {
            log.error("Failed to send email: type={}, to={}, error={}", templateName, to, e.message, e)
            throw EmailSendException("Failed to send $templateName email to $to", e)
        }
    }

    /**
     * Send via Resend using a pre-configured remote template. Local HTML rendering is skipped.
     */
    private fun sendViaResend(
        to: String,
        subject: String,
        templateName: String,
        resendTemplate: ResendProperties.ResendTemplate,
        variables: Map<String, Any>
    ) {
        // Map internal variable names to the Resend template's variable names.
        // Internal variables without an explicit mapping are passed through under their own name.
        val resendVariables = variables.entries.associate { (key, value) ->
            (resendTemplate.variables[key] ?: key) to value
        }

        val payload = linkedMapOf<String, Any>(
            "from" to resendProperties.from.ifBlank { mailProperties.from },
            "to" to listOf(to),
            "subject" to subject,
            "template_id" to resendTemplate.id,
            "variables" to resendVariables
        )

        try {
            val response = HttpUtil.createPost("${resendProperties.apiBaseUrl.trimEnd('/')}/emails")
                .header("Authorization", "Bearer ${resendProperties.apiKey}")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(payload))
                .execute()

            if (!response.isOk) {
                throw EmailSendException(
                    "Resend API rejected $templateName email to $to: HTTP ${response.status} - ${response.body()}"
                )
            }
            log.info(
                "Email sent successfully: type={}, to={}, channel=resend, id={}",
                templateName,
                to,
                runCatching { JSONUtil.parseObj(response.body()).getStr("id") }.getOrDefault("")
            )
        } catch (e: EmailSendException) {
            log.error("Failed to send email: type={}, to={}, error={}", templateName, to, e.message, e)
            throw e
        } catch (e: Exception) {
            log.error("Failed to send email: type={}, to={}, error={}", templateName, to, e.message, e)
            throw EmailSendException("Failed to send $templateName email to $to via Resend", e)
        }
    }
}

/**
 * Exception thrown when email sending fails.
 */
class EmailSendException : RuntimeException {
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String) : super(message)
}
