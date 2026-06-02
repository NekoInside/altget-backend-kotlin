package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.CoinToken
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.mapper.db.coin.CoinTokenMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.apply

@Service
class CoinTokenService(
    private val userCoinService: UserCoinService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService
) : ServiceImpl<CoinTokenMapper, CoinToken>() {
    @Transactional(rollbackFor = [Exception::class])
    fun redeemTokenForUser(token: String, userIn: Int): Boolean {
        val token = query()
            .eq("token", token)
            .last("FOR UPDATE")
            .one()
        if (token == null || token.redeemedBy != null) return false
        val userCoin = userCoinService.getById(userIn) ?: return false
        userCoinService.updateById(userCoin.apply {
            balance += token.coinAmount
        })
        updateById(token.apply {
            redeemedBy = userIn
            redeemedAt = LocalDateTime.now()
            isUsed = true
        })
        coinTransactionHistoryService.logTransaction(
            userId = userIn,
            amount = token.coinAmount,
            type = EnumTransactionType.TOKEN_REDEEM,
            relatedTokenId = token.id
        )
        return true
    }
}