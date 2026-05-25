package ltd.guimc.web.altget.service.convert

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.coin.CoinTransactionHistory
import ltd.guimc.web.altget.mapper.coin.CoinTransactionHistoryMapper
import org.springframework.stereotype.Service

@Service
class ConvertTaskService : ServiceImpl<CoinTransactionHistoryMapper, CoinTransactionHistory>() {
}