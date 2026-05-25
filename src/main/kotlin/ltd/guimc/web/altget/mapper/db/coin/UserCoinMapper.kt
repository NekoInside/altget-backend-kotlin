package ltd.guimc.web.altget.mapper.db.coin

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserCoinMapper : BaseMapper<UserCoin> {
}