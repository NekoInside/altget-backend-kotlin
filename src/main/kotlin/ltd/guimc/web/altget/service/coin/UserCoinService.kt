package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.mapper.db.coin.UserCoinMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCoinService(private val coinTransactionHistoryService: CoinTransactionHistoryService) : ServiceImpl<UserCoinMapper, UserCoin>() {
    fun addBalance(userId: Int, amount: Long): Boolean {
        require(amount > 0) { "Amount must be positive" }
        return baseMapper.addBalance(userId, amount, Long.MAX_VALUE - amount) == 1
    }

    fun subtractBalance(userId: Int, amount: Long): Boolean {
        require(amount > 0) { "Amount must be positive" }
        return baseMapper.subtractBalance(userId, amount) == 1
    }

    @Transactional
    fun transfer(fromId: Int, toId: Int, amount: Int) {
        require(amount > 0) { "转账金额必须为正数" }
        if (!subtractBalance(fromId, amount.toLong())) throw IllegalArgumentException("用户 $fromId 的余额不足或不存在")
        if (!addBalance(toId, amount.toLong())) throw IllegalArgumentException("用户 $toId 不存在或余额超出限制")
        coinTransactionHistoryService.logTransaction(userId = fromId, amount = -amount, type = EnumTransactionType.TRANSFER_SENT)
        coinTransactionHistoryService.logTransaction(userId = toId, amount = amount, type = EnumTransactionType.TRANSFER_RECEIVED)
    }
}
