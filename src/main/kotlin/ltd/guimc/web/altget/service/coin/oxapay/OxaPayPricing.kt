package ltd.guimc.web.altget.service.coin.oxapay

import java.math.BigDecimal
import java.math.RoundingMode

object OxaPayPricing {
    val USD_TO_CNY_RATE: BigDecimal = BigDecimal("6.5")
    const val COINS_PER_CNY: Int = 1_000
    private const val COINS_PER_USD_CENT: Long = 65

    data class Price(
        val usdAmount: BigDecimal,
        val cnyAmount: BigDecimal,
        val coinAmount: Int,
    )

    fun calculate(usdAmount: BigDecimal): Price {
        val normalizedUsd = try {
            usdAmount.setScale(2, RoundingMode.UNNECESSARY)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("USD amount supports at most 2 decimal places")
        }
        require(normalizedUsd > BigDecimal.ZERO) { "USD amount must be at least 0.01" }

        val coins = try {
            val usdCents = normalizedUsd.movePointRight(2).longValueExact()
            Math.multiplyExact(usdCents, COINS_PER_USD_CENT)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("USD amount is too large")
        }
        require(coins <= Int.MAX_VALUE) { "USD amount is too large" }

        return Price(
            usdAmount = normalizedUsd,
            cnyAmount = normalizedUsd.multiply(USD_TO_CNY_RATE).setScale(3),
            coinAmount = coins.toInt(),
        )
    }
}
