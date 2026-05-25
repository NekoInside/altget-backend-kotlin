package ltd.guimc.web.altget.mapper.coin

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.coin.CoinTransactionHistory
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CoinTransactionHistoryMapper : BaseMapper<CoinTransactionHistory> {
}