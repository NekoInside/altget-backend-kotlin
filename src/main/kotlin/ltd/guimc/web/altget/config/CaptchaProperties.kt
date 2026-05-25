package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Captcha provider configurations, bound from `captcha.*` in application.yaml.
 *
 * Extensible: add new nested config classes for additional captcha providers
 * (e.g. hCaptcha, Cloudflare Turnstile) under the same `captcha` prefix.
 */
@ConfigurationProperties(prefix = "captcha")
data class CaptchaProperties(
    val geetest: Geetest = Geetest()
) {

    /**
     * GeeTest v4 (行为验证) configuration.
     */
    data class Geetest(
        /** GeeTest captcha ID, obtained from GeeTest dashboard */
        var id: String = "",

        /** GeeTest captcha key (secret), obtained from GeeTest dashboard */
        var key: String = "",

        /** GeeTest validation API endpoint */
        var apiServer: String = "https://gcaptcha4.geetest.com/validate"
    )
}
