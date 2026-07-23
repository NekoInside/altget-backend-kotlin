package ltd.guimc.web.altget.entity.response.coin

import ltd.guimc.web.altget.entity.db.coin.OxaPayRechargeOrder
import ltd.guimc.web.altget.enum.EnumOxaPayRechargeStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class OxaPayRechargeResponse(
    val orderId: String,
    val trackId: String?,
    val usdAmount: BigDecimal,
    val cnyAmount: BigDecimal,
    val coinAmount: Int,
    val status: EnumOxaPayRechargeStatus,
    val paymentUrl: String?,
    val expiredAt: Instant?,
    val paidAt: LocalDateTime?,
) {
    companion object {
        fun from(order: OxaPayRechargeOrder) = OxaPayRechargeResponse(
            orderId = order.id,
            trackId = order.trackId,
            usdAmount = order.usdAmount,
            cnyAmount = order.cnyAmount,
            coinAmount = order.coinAmount,
            status = order.statusAt(),
            paymentUrl = order.paymentUrl,
            expiredAt = order.expiredAt?.toInstant(ZoneOffset.UTC),
            paidAt = order.paidAt,
        )
    }
}
