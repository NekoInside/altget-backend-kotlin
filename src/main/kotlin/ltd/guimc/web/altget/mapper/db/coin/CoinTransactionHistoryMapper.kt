package ltd.guimc.web.altget.mapper.db.coin

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.coin.CoinTransactionHistory
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CoinTransactionHistoryMapper : BaseMapper<CoinTransactionHistory> {
}