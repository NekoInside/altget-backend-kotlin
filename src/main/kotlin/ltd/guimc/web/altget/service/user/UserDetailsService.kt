package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.mapper.db.user.UserDetailsMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service

@Service
class UserDetailsService: ServiceImpl<UserDetailsMapper, UserDetails>(), IPageService<UserDetails> {
}
