package ltd.guimc.web.altget.mapper.coin

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.coin.CoinToken
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CoinTokenMapper : BaseMapper<CoinToken> {
}