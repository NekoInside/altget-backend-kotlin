package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.coin.UserCoin
import ltd.guimc.web.altget.mapper.coin.UserCoinMapper
import org.springframework.stereotype.Service

@Service
class UserCoinService : ServiceImpl<UserCoinMapper, UserCoin>() {
}