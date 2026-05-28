package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import ltd.guimc.web.altget.entity.db.alt.AltCategory
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.service.coin.UserCoinService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PayForAltService(private val userCoinService: UserCoinService, private val altService: AltService) {
    @Transactional(rollbackFor = [Exception::class])
    fun payForAltAs(count: Int = 1, userId: Int): List<AltCategory> {
        val userCoinWrapper = LambdaQueryWrapper<UserCoin>()
            .eq(UserCoin::userId, userId)
            .last("FOR UPDATE")
        val userCoin = userCoinService.getOne(userCoinWrapper) ?: throw RuntimeException("用户不存在")
        if (userCoin.balance == null) throw RuntimeException("用户余额异常")
        if (userCoin.balance!! < count) {
            throw RuntimeException("余额不足")
        }
        val popupData = altService.fetchAlt(count)
        val correctCount = popupData.size
        val cost = correctCount;
        if (cost > 0) {
            userCoin.balance = userCoin.balance!! - cost
            userCoinService.updateById(userCoin)
        }
        return popupData
    }
}