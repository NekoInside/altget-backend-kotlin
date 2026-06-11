package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.alt.AltCategory
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.mapper.db.alt.AltCategoryMapper
import ltd.guimc.web.altget.service.coin.CoinTransactionHistoryService
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.sauth.SauthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AltService(
    private val altConsumptionRecordService: AltConsumptionRecordService,
    private val userCoinService: UserCoinService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService,
    private val sauthService: SauthService
) : ServiceImpl<AltCategoryMapper, AltCategory>() {

    /**
     * Self-reference via proxy to enable @Transactional on inner method calls.
     * Without this, direct calls like `deductCoinForSauth()` bypass the AOP proxy
     * and the transaction annotation has no effect at runtime.
     */
    @Autowired
    @Lazy
    private lateinit var self: AltService

    private val log = LoggerFactory.getLogger(AltService::class.java)

    @Transactional(rollbackFor = [Exception::class])
    fun fetchAlt(count: Int = 1, channel: String = "default"): List<AltCategory> {
        val popupData = baseMapper.popupByChannel(channel, count)
        removeBatchByIds(popupData.map { it.id })
        if (popupData.isNotEmpty()) {
            altConsumptionRecordService.recordConsumption(channel, popupData.size)
        }
        return popupData
    }

    /**
     * 扣除 1 个硬币（在短事务中执行，不包含 HTTP 调用）
     *
     * @param userId 用户 ID
     * @throws RuntimeException 如果余额不足或用户不存在
     */
    @Transactional(rollbackFor = [Exception::class])
    fun deductCoinForSauth(userId: Int) {
        val userCoinWrapper = QueryWrapper<UserCoin>()
            .eq("user_id", userId)
            .last("FOR UPDATE")
        val userCoin = userCoinService.getOne(userCoinWrapper) ?: throw RuntimeException("用户不存在")
        if (userCoin.balance < 1) {
            throw RuntimeException("余额不足")
        }
        userCoin.balance -= 1
        userCoinService.updateById(userCoin)
        coinTransactionHistoryService.logTransaction(
            userId = userId,
            amount = -1,
            type = EnumTransactionType.SAUTH_CONVERT
        )
    }

    /**
     * 退还 sauth 转换扣除的 1 个硬币（在独立短事务中执行）
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = [Exception::class])
    fun refundCoinForSauth(userId: Int) {
        val userCoinWrapper = QueryWrapper<UserCoin>()
            .eq("user_id", userId)
            .last("FOR UPDATE")
        val userCoin = userCoinService.getOne(userCoinWrapper) ?: return
        userCoin.balance += 1
        userCoinService.updateById(userCoin)
        coinTransactionHistoryService.logTransaction(
            userId = userId,
            amount = 1,
            type = EnumTransactionType.SAUTH_CONVERT
        )
    }

    /**
     * 将指定的账号转换为 sauth（4399 登录），消耗 1 个硬币
     *
     * 硬币扣除在独立短事务中执行，HTTP 调用在事务外执行，避免长时间占用数据库连接和行锁。
     * 如果 HTTP 调用失败或 sauth 返回 success=false，将自动退还硬币。
     *
     * @param userId   用户 ID
     * @param username 4399 用户名
     * @param password 密码
     * @return sauth 登录结果 Map（包含 success, message, sauthJson）
     */
    fun convertAltToSauth(userId: Int, username: String, password: String): Map<String, Any?> {
        // 第一步：在短事务中扣除硬币（不包含 HTTP 调用）
        // Use self (AOP proxy) to ensure @Transactional is applied
        self.deductCoinForSauth(userId)
        // 第二步：在事务外调用 HTTP 请求，避免长时间占用数据库连接
        return try {
            val result = sauthService.doLogin(username, password)
            val success = result["success"] as? Boolean ?: false
            if (!success) {
                // 登录失败，退还硬币
                log.warn("Sauth login returned failure for user {}, refunding coin", userId)
                self.refundCoinForSauth(userId)
            }
            result
        } catch (e: Exception) {
            log.error("Sauth login failed after coin deduction for user {}", userId, e)
            // HTTP 调用失败，退还硬币
            self.refundCoinForSauth(userId)
            throw RuntimeException("4399 登录服务暂时不可用，请稍后再试")
        }
    }
}
