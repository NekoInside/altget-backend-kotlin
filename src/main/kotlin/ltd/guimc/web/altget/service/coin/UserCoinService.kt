package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.mapper.db.coin.UserCoinMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCoinService : ServiceImpl<UserCoinMapper, UserCoin>() {
    @Transactional
    fun transfer(fromId: Int, toId: Int, amount: Int) {
        val fromUserCoin = getById(fromId) ?: throw IllegalArgumentException("用户 $fromId 不存在")
        val toUserCoin = getById(toId) ?: throw IllegalArgumentException("用户 $toId 不存在")

        if (fromUserCoin.balance < amount) {
            throw IllegalArgumentException("用户 $fromId 的余额不足")
        }

        fromUserCoin.balance -= amount
        toUserCoin.balance += amount

        updateById(fromUserCoin)
        updateById(toUserCoin)
    }
}