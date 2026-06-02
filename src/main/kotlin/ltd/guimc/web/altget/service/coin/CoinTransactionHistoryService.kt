package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.CoinTransactionHistory
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.mapper.db.coin.CoinTransactionHistoryMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service

@Service
class CoinTransactionHistoryService
: ServiceImpl<CoinTransactionHistoryMapper, CoinTransactionHistory>(), IPageService<CoinTransactionHistory> {
    fun logTransaction(userId: Int, amount: Int, type: EnumTransactionType, relatedTokenId: String = "") {
        save(CoinTransactionHistory().apply {
            this.userId = userId
            this.amount = amount
            this.transactionType = type
            this.relatedTokenId = relatedTokenId
        })
    }
}