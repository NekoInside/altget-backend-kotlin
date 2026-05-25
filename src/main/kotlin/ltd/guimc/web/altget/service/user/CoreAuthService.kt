package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.mapper.db.user.CoreAuthMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service

@Service
class CoreAuthService : ServiceImpl<CoreAuthMapper, CoreAuth>(), IPageService<CoreAuth> {
    fun getByUsername(username: String): CoreAuth? {
        val queryWrapper = lambdaQuery().eq(CoreAuth::username, username)
        // 先检查有没有这个用户
        val count = count(queryWrapper)
        if (count.toInt() == 0) {
            return null
        }
        return getOne(queryWrapper)
    }

    fun getByEmail(email: String): CoreAuth? {
        val queryWrapper = lambdaQuery().eq(CoreAuth::email, email)
        val count = count(queryWrapper)
        if (count.toInt() == 0) {
            return null
        }
        return getOne(queryWrapper)
    }
}