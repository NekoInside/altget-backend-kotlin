package ltd.guimc.web.altget.entity.response.coin

import ltd.guimc.web.altget.entity.db.coin.OxaPayRechargeOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

class OxaPayRechargeResponseTest {
    @Test
    fun `exposes invoice expiration as a UTC instant`() {
        val order = OxaPayRechargeOrder().apply {
            expiredAt = LocalDateTime.of(2026, 7, 22, 16, 0)
        }

        val response = OxaPayRechargeResponse.from(order)

        assertEquals(Instant.parse("2026-07-22T16:00:00Z"), response.expiredAt)
    }
}
