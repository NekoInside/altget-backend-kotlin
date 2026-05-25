package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.CoinTransactionHistory
import ltd.guimc.web.altget.mapper.db.coin.CoinTransactionHistoryMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service

@Service
class CoinTransactionHistoryService
:
    ServiceImpl<CoinTransactionHistoryMapper, CoinTransactionHistory>(),
    IPageService<CoinTransactionHistory> {
}