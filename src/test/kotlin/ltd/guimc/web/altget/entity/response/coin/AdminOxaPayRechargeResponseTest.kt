package ltd.guimc.web.altget.entity.response.coin

import ltd.guimc.web.altget.entity.db.coin.OxaPayRechargeOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

class AdminOxaPayRechargeResponseTest {
    @Test
    fun `maps admin order fields and exposes expiration as UTC`() {
        val order = OxaPayRechargeOrder().apply {
            userId = 42
            expiredAt = LocalDateTime.of(2026, 7, 22, 16, 0)
        }

        val response = AdminOxaPayRechargeResponse.from(order)

        assertEquals(42, response.userId)
        assertEquals(Instant.parse("2026-07-22T16:00:00Z"), response.expiredAt)
    }
}
