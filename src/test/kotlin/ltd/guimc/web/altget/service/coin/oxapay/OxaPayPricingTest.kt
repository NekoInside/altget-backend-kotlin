package ltd.guimc.web.altget.service.coin.oxapay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OxaPayPricingTest {
    @Test
    fun `converts USD to CNY and coins using fixed rates`() {
        val price = OxaPayPricing.calculate(BigDecimal("1.00"))

        assertEquals(BigDecimal("1.00"), price.usdAmount)
        assertEquals(BigDecimal("6.500"), price.cnyAmount)
        assertEquals(6_500, price.coinAmount)
    }

    @Test
    fun `converts one USD cent to an exact coin amount`() {
        val price = OxaPayPricing.calculate(BigDecimal("0.01"))

        assertEquals(BigDecimal("0.065"), price.cnyAmount)
        assertEquals(65, price.coinAmount)
    }

    @Test
    fun `rejects fractions smaller than one USD cent`() {
        assertThrows(IllegalArgumentException::class.java) {
            OxaPayPricing.calculate(BigDecimal("0.001"))
        }
    }

    @Test
    fun `rejects amounts that overflow the coin balance type`() {
        assertThrows(IllegalArgumentException::class.java) {
            OxaPayPricing.calculate(BigDecimal("100000000000000000000.00"))
        }
    }
}
