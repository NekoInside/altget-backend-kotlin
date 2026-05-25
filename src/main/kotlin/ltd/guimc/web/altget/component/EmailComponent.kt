package ltd.guimc.web.altget.component

import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import ltd.guimc.web.altget.config.MailProperties
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

/**
 * Component responsible for sending templated HTML emails using Thymeleaf.
 *
 * Templates are resolved from `src/main/resources/templates/email/`.
 * Available templates: [activation], [password-reset].
 */
@Component
class EmailComponent(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,
    private val mailProperties: MailProperties
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
        val context = Context().apply {
            setVariable("username", username)
            setVariable("activationUrl", activationUrl)
            setVariable("expirationMinutes", expirationMinutes)
            setVariable("domain", mailProperties.domain)
        }
        sendTemplateEmail(to, "账户激活确认 - 凌清阁", "activation", context)
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
        val context = Context().apply {
            setVariable("username", username)
            setVariable("resetUrl", resetUrl)
            setVariable("expirationMinutes", expirationMinutes)
            setVariable("domain", mailProperties.domain)
        }
        sendTemplateEmail(to, "密码重置请求 - 凌清阁", "password-reset", context)
    }

    /**
     * Generic method to send an HTML email rendered from a Thymeleaf template.
     *
     * @param to          recipient email address
     * @param subject     email subject line
     * @param templateName template name without the `.html` extension (relative to `email/`)
     * @param context     Thymeleaf [Context] containing template variables
     */
    fun sendTemplateEmail(
        to: String,
        subject: String,
        templateName: String,
        context: Context
    ) {
        try {
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom(mailProperties.from)
            helper.setTo(to)
            helper.setSubject(subject)

            val htmlContent = templateEngine.process("$TEMPLATE_PATH_PREFIX$templateName", context)
            helper.setText(htmlContent, true)

            mailSender.send(mimeMessage)
            log.info("Email sent successfully: type={}, to={}", templateName, to)
        } catch (e: MessagingException) {
            log.error("Failed to send email: type={}, to={}, error={}", templateName, to, e.message, e)
            throw EmailSendException("Failed to send $templateName email to $to", e)
        }
    }
}

/**
 * Exception thrown when email sending fails.
 */
class EmailSendException(message: String, cause: Throwable) : RuntimeException(message, cause)
