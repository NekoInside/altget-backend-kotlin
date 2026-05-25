package ltd.guimc.web.altget.service.coin

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.coin.CoinToken
import ltd.guimc.web.altget.mapper.db.coin.CoinTokenMapper
import org.springframework.stereotype.Service

@Service
class CoinTokenService : ServiceImpl<CoinTokenMapper, CoinToken>() {
}