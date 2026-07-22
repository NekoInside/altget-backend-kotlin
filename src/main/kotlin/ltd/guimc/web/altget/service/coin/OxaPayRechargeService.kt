package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.OxaPayRechargeOrder
import ltd.guimc.web.altget.enum.EnumOxaPayRechargeStatus
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.mapper.db.coin.OxaPayRechargeOrderMapper
import ltd.guimc.web.altget.service.coin.oxapay.OxaPayCallback
import ltd.guimc.web.altget.service.coin.oxapay.OxaPayClient
import ltd.guimc.web.altget.service.coin.oxapay.OxaPayPricing
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OxaPayRechargeService(
    private val oxaPayClient: OxaPayClient,
    private val userCoinService: UserCoinService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService,
) : ServiceImpl<OxaPayRechargeOrderMapper, OxaPayRechargeOrder>(), IPageService<OxaPayRechargeOrder> {
    fun create(userId: Int, usdAmount: BigDecimal): OxaPayRechargeOrder {
        val price = OxaPayPricing.calculate(usdAmount)
        val order = OxaPayRechargeOrder().apply {
            this.userId = userId
            this.usdAmount = price.usdAmount
            this.cnyAmount = price.cnyAmount
            this.coinAmount = price.coinAmount
        }
        save(order)

        try {
            val invoice = oxaPayClient.createInvoice(order)
            update(
                UpdateWrapper<OxaPayRechargeOrder>()
                    .eq("id", order.id)
                    .set("track_id", invoice.trackId)
                    .set("payment_url", invoice.paymentUrl)
                    .set("expired_at", invoice.expiredAt)
            )
            order.trackId = invoice.trackId
            order.paymentUrl = invoice.paymentUrl
            order.expiredAt = invoice.expiredAt
            return order
        } catch (e: Exception) {
            update(
                UpdateWrapper<OxaPayRechargeOrder>()
                    .eq("id", order.id)
                    .ne("status", EnumOxaPayRechargeStatus.PAID.value)
                    .set("status", EnumOxaPayRechargeStatus.CREATE_FAILED.value)
            )
            throw e
        }
    }

    fun getForUser(orderId: String, userId: Int): OxaPayRechargeOrder? = getOne(
        QueryWrapper<OxaPayRechargeOrder>()
            .eq("id", orderId)
            .eq("user_id", userId)
    )

    @Transactional(rollbackFor = [Exception::class])
    fun settlePaidCallback(callback: OxaPayCallback) {
        val order = getOne(
            QueryWrapper<OxaPayRechargeOrder>()
                .eq("id", callback.orderId)
                .last("FOR UPDATE")
        ) ?: throw IllegalArgumentException("Unknown OxaPay order")

        require(callback.type.equals("invoice", ignoreCase = true)) { "Unexpected OxaPay payment type" }
        require(callback.currency.equals("USD", ignoreCase = true)) { "Unexpected OxaPay payment currency" }
        require(callback.amount.compareTo(order.usdAmount) == 0) { "OxaPay payment amount does not match the order" }
        require(order.trackId == null || order.trackId == callback.trackId) { "OxaPay track ID does not match the order" }
        if (order.status == EnumOxaPayRechargeStatus.PAID) return

        check(userCoinService.addBalance(order.userId, order.coinAmount.toLong())) {
            "User coin data not found or balance would overflow"
        }
        coinTransactionHistoryService.logTransaction(
            userId = order.userId,
            amount = order.coinAmount,
            type = EnumTransactionType.OXAPAY_RECHARGE,
        )
        order.trackId = callback.trackId
        order.status = EnumOxaPayRechargeStatus.PAID
        order.paidAt = LocalDateTime.now()
        updateById(order)
    }
}
