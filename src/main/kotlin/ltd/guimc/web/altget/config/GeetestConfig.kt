package ltd.guimc.web.altget.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Enables [CaptchaProperties] for type-safe binding of captcha-related
 * configuration from application.yaml.
 */
@Configuration
@EnableConfigurationProperties(CaptchaProperties::class)
class GeetestConfig
