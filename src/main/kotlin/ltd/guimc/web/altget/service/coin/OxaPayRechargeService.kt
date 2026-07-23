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
import java.time.ZoneOffset

@Service
class OxaPayRechargeService(
    private val oxaPayClient: OxaPayClient,
    private val userCoinService: UserCoinService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService,
) : ServiceImpl<OxaPayRechargeOrderMapper, OxaPayRechargeOrder>(), IPageService<OxaPayRechargeOrder> {
    data class TopUpResult(
        val order: OxaPayRechargeOrder,
        val credited: Boolean,
    )

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
    )?.also { order ->
        markExpired(order)
    }

    fun markExpiredOrders(now: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): Int {
        val wrapper = UpdateWrapper<OxaPayRechargeOrder>()
            .eq("status", EnumOxaPayRechargeStatus.PENDING.value)
            .isNotNull("expired_at")
            .le("expired_at", now)
            .set("status", EnumOxaPayRechargeStatus.EXPIRED.value)
        return baseMapper.update(null, wrapper)
    }

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
        if (order.status == EnumOxaPayRechargeStatus.PAID ||
            order.status == EnumOxaPayRechargeStatus.PAID_AFTER_EXPIRATION
        ) return
        if (order.statusAt(now = LocalDateTime.now(ZoneOffset.UTC)) == EnumOxaPayRechargeStatus.EXPIRED) {
            order.trackId = callback.trackId
            order.status = EnumOxaPayRechargeStatus.PAID_AFTER_EXPIRATION
            order.paidAt = LocalDateTime.now()
            updateById(order)
            return
        }

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

    @Transactional(rollbackFor = [Exception::class])
    fun updateStatus(orderId: String, targetStatus: EnumOxaPayRechargeStatus): OxaPayRechargeOrder? {
        val order = getOrderForUpdate(orderId) ?: return null
        if (order.status == EnumOxaPayRechargeStatus.PAID && targetStatus != EnumOxaPayRechargeStatus.PAID) {
            throw IllegalStateException("Paid OxaPay order status cannot be changed")
        }
        if (targetStatus == EnumOxaPayRechargeStatus.PAID && order.status != EnumOxaPayRechargeStatus.PAID) {
            throw IllegalStateException("Use the top-up endpoint to settle an unpaid OxaPay order")
        }
        if (targetStatus == EnumOxaPayRechargeStatus.PENDING && order.statusAt() == EnumOxaPayRechargeStatus.EXPIRED) {
            throw IllegalStateException("The OxaPay order has already expired")
        }
        if (order.status != targetStatus) {
            order.status = targetStatus
            updateById(order)
        }
        return order
    }

    @Transactional(rollbackFor = [Exception::class])
    fun topUp(orderId: String): TopUpResult? {
        val order = getOrderForUpdate(orderId) ?: return null
        if (order.status == EnumOxaPayRechargeStatus.PAID) return TopUpResult(order, credited = false)
        if (order.status == EnumOxaPayRechargeStatus.PENDING &&
            order.statusAt() == EnumOxaPayRechargeStatus.EXPIRED
        ) {
            order.status = EnumOxaPayRechargeStatus.EXPIRED
        }
        require(
            order.status == EnumOxaPayRechargeStatus.EXPIRED ||
                order.status == EnumOxaPayRechargeStatus.PAID_AFTER_EXPIRATION
        ) { "Only an expired OxaPay order can be topped up manually" }

        check(userCoinService.addBalance(order.userId, order.coinAmount.toLong())) {
            "User coin data not found or balance would overflow"
        }
        coinTransactionHistoryService.logTransaction(
            userId = order.userId,
            amount = order.coinAmount,
            type = EnumTransactionType.OXAPAY_RECHARGE_MANUAL,
            relatedTokenId = order.id,
        )
        order.status = EnumOxaPayRechargeStatus.PAID
        order.paidAt = order.paidAt ?: LocalDateTime.now()
        updateById(order)
        return TopUpResult(order, credited = true)
    }

    private fun getOrderForUpdate(orderId: String): OxaPayRechargeOrder? = getOne(
        QueryWrapper<OxaPayRechargeOrder>()
            .eq("id", orderId)
            .last("FOR UPDATE")
    )

    private fun markExpired(order: OxaPayRechargeOrder) {
        if (order.statusAt() != EnumOxaPayRechargeStatus.EXPIRED) return
        if (order.status == EnumOxaPayRechargeStatus.PENDING) {
            order.status = EnumOxaPayRechargeStatus.EXPIRED
            updateById(order)
        }
    }
}
