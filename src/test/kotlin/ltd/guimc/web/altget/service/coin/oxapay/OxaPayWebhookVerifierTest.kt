package ltd.guimc.web.altget.service.coin.oxapay

import ltd.guimc.web.altget.config.OxaPayProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class OxaPayWebhookVerifierTest {
    private val verifier = OxaPayWebhookVerifier(OxaPayProperties(merchantApiKey = "test-secret"))
    private val body = "{\"track_id\":\"123\",\"status\":\"Paid\"}".toByteArray(StandardCharsets.UTF_8)

    @Test
    fun `accepts a valid raw-body HMAC SHA512 signature`() {
        val signature = "4f6932055a31df43f6103eaa8df93a956b6f531e65005cc12a30745532582787a8a04e55acb835804684340807a0a249d6c2a893f41ad9fa50740a2f5eeb4fe8"

        assertTrue(verifier.verify(body, signature))
    }

    @Test
    fun `rejects malformed and changed signatures`() {
        assertFalse(verifier.verify(body, null))
        assertFalse(verifier.verify(body, "not-hex"))
        assertFalse(verifier.verify(body + ' '.code.toByte(), "4f6932055a31df43f6103eaa8df93a956b6f531e65005cc12a30745532582787a8a04e55acb835804684340807a0a249d6c2a893f41ad9fa50740a2f5eeb4fe8"))
    }
}
