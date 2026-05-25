package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.user.CoreAuth
import ltd.guimc.web.altget.mapper.user.CoreAuthMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service

@Service
class CoreAuthService : ServiceImpl<CoreAuthMapper, CoreAuth>(), IPageService<CoreAuth> {
}