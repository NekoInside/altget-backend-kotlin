package ltd.guimc.web.altget.entity.db.coin

import ltd.guimc.web.altget.enum.EnumOxaPayRechargeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OxaPayRechargeOrderTest {
    private val expiration = LocalDateTime.of(2026, 7, 24, 12, 0)

    @Test
    fun `pending order is expired at invoice expiration time`() {
        val order = OxaPayRechargeOrder().apply {
            expiredAt = expiration
        }

        assertEquals(EnumOxaPayRechargeStatus.PENDING, order.statusAt(expiration.minusNanos(1)))
        assertEquals(EnumOxaPayRechargeStatus.EXPIRED, order.statusAt(expiration))
    }

    @Test
    fun `paid order remains paid after invoice expiration`() {
        val order = OxaPayRechargeOrder().apply {
            status = EnumOxaPayRechargeStatus.PAID
            expiredAt = expiration
        }

        assertEquals(EnumOxaPayRechargeStatus.PAID, order.statusAt(expiration.plusDays(1)))
    }

    @Test
    fun `payment received after expiration keeps a distinct status`() {
        val order = OxaPayRechargeOrder().apply {
            status = EnumOxaPayRechargeStatus.PAID_AFTER_EXPIRATION
            expiredAt = expiration
        }

        assertEquals(EnumOxaPayRechargeStatus.PAID_AFTER_EXPIRATION, order.statusAt(expiration.plusDays(1)))
    }
}
