package ltd.guimc.web.altget.service.coin.oxapay

import ltd.guimc.web.altget.config.OxaPayProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class OxaPayWebhookVerifier(private val properties: OxaPayProperties) {
    fun verify(rawBody: ByteArray, signature: String?): Boolean {
        if (properties.merchantApiKey.isBlank() || signature == null || signature.length != 128) return false

        val suppliedSignature = try {
            HexFormat.of().parseHex(signature)
        } catch (_: IllegalArgumentException) {
            return false
        }
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(properties.merchantApiKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA512"))
        return MessageDigest.isEqual(mac.doFinal(rawBody), suppliedSignature)
    }
}
