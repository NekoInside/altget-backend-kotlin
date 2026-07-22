package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.CoinToken
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.mapper.db.coin.CoinTokenMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.apply

@Service
class CoinTokenService(
    private val userCoinService: UserCoinService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService
) : ServiceImpl<CoinTokenMapper, CoinToken>(), IPageService<CoinToken> {
    @Transactional(rollbackFor = [Exception::class])
    fun redeemTokenForUser(token: String, userIn: Int): Boolean {
        val token = getOne(QueryWrapper<CoinToken>()
            .eq("id", token)
            .last("FOR UPDATE"))
        if (token == null || token.redeemedBy != null) return false
        if (!userCoinService.addBalance(userIn, token.coinAmount.toLong())) return false
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
