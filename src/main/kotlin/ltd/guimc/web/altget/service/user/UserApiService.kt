package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.user.UserApi
import ltd.guimc.web.altget.mapper.user.UserApiMapper
import org.springframework.stereotype.Service

@Service
class UserApiService : ServiceImpl<UserApiMapper, UserApi>() {
}