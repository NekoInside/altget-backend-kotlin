package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserOauth
import ltd.guimc.web.altget.mapper.db.user.UserOauthMapper
import org.springframework.stereotype.Service

@Service
class UserOauthService : ServiceImpl<UserOauthMapper, UserOauth>() {
}