package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.mapper.db.user.CoreAuthMapper
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoreAuthService(
    private val userCoinService: UserCoinService,
    private val userDetailsService: UserDetailsService,
    private val userOauthService: UserOauthService,
    private val userOperationService: UserOperationService
) : ServiceImpl<CoreAuthMapper, CoreAuth>(), IPageService<CoreAuth> {
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

    @Transactional(rollbackFor = [Exception::class])
    fun removeAccount(userId: Int) {
        userCoinService.removeById(userId)
        userDetailsService.removeById(userId)
        userOauthService.removeById(userId)
        userOperationService.removeByUserId(userId)
        removeById(userId)
    }
}