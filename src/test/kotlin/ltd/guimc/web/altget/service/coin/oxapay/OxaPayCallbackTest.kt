package ltd.guimc.web.altget.service.coin.oxapay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class OxaPayCallbackTest {
    @Test
    fun `parses invoice callback fields used for settlement`() {
        val body = """{"track_id":"193139644","status":"Paid","type":"invoice","amount":1.25,"currency":"USD","order_id":"order-1"}"""

        val callback = OxaPayCallback.parse(body.toByteArray(StandardCharsets.UTF_8))

        assertEquals("193139644", callback.trackId)
        assertEquals("Paid", callback.status)
        assertEquals("invoice", callback.type)
        assertEquals(0, BigDecimal("1.25").compareTo(callback.amount))
        assertEquals("USD", callback.currency)
        assertEquals("order-1", callback.orderId)
    }
}
