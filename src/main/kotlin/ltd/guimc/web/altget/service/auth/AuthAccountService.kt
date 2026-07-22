package ltd.guimc.web.altget.service.auth

import ltd.guimc.web.altget.component.EmailComponent
import ltd.guimc.web.altget.component.JwtTokenComponent
import ltd.guimc.web.altget.config.SiteProperities
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.entity.db.user.UserOauth
import ltd.guimc.web.altget.entity.request.auth.ForgotPasswordRequest
import ltd.guimc.web.altget.entity.request.auth.RegisterRequest
import ltd.guimc.web.altget.entity.request.auth.ResetPasswordRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOauthService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder

@Service
class AuthAccountService(
    private val emailComponent: EmailComponent,
    private val jwtTokenComponent: JwtTokenComponent,
    private val siteProperities: SiteProperities,
    private val coreAuthService: CoreAuthService,
    private val userOauthService: UserOauthService,
    private val userCoinService: UserCoinService,
    @Qualifier("objectRedisTemplate")
    private val objectRedisTemplate: RedisTemplate<String, Any>,
    private val userDetailsService: UserDetailsService,
    private val userOperationService: UserOperationService,
    private val authEnumerationProtectionService: AuthEnumerationProtectionService
) {
    @Transactional
    fun register(request: RegisterRequest): ResponseBase<String> {
        if (coreAuthService.getByUsername(request.username) != null ||
            coreAuthService.getByEmail(request.email) != null) {
            authEnumerationProtectionService.simulateRegister(request)
            return ResponseBase("若该用户名或邮箱未被注册，我们将发送一封验证邮件到该邮箱，请注意查收")
        }

        coreAuthService.save(CoreAuth().apply {
            email = request.email
            username = request.username
            srpSalt = request.salt
            srpVerifier = request.verifier
        })

        val savedCoreAuth =
            coreAuthService.getByUsername(request.username) ?: return ResponseBase(500, "用户注册失败，请稍后再试")

        userOauthService.save(UserOauth().apply {
            userId = savedCoreAuth.userId
        })
        userCoinService.save(UserCoin().apply {
            userId = savedCoreAuth.userId
            balance = 0L
        })
        userDetailsService.save(UserDetails().apply {
            userId = savedCoreAuth.userId
        })

        val token = jwtTokenComponent.generateRegisterVerifyToken(request.email)
        val fullActiveUrl = "https://${siteProperities.domain}/activate?token=" + URLEncoder.encode(token, "UTF-8")
        emailComponent.sendActivationEmail(request.email, request.username, fullActiveUrl)
        return ResponseBase("若该用户名或邮箱未被注册，我们将发送一封验证邮件到该邮箱，请注意查收")
    }

    fun activate(code: String): ResponseBase<String> {
        val email = jwtTokenComponent.getEmailFromToken(code) ?: return ResponseBase(400, "无效的激活链接")
        val coreAuthEntity = coreAuthService.getByEmail(email) ?: return ResponseBase(400, "无效的激活链接")
        val userDetails = userDetailsService.getById(coreAuthEntity.userId) ?: return ResponseBase(400, "无效的激活链接")
        if (userDetails.userRole != EnumUserRole.UNVERIFY) return ResponseBase(400, "无效的激活链接")

        userDetailsService.updateById(userDetails.apply {
            userRole = EnumUserRole.VERIFY
        })
        return ResponseBase("账户激活成功，您现在可以使用账号密码登录了")
    }

    fun forgotPassword(request: ForgotPasswordRequest): ResponseBase<String> {
        val coreAuth = coreAuthService.getByEmail(request.email)
        if (coreAuth != null) {
            val token = jwtTokenComponent.generatePasswordResetToken(request.email)
            val resetId = jwtTokenComponent.getPasswordResetIdFromToken(token)
            if (resetId != null) {
                objectRedisTemplate.opsForValue().set(
                    "password-reset:$resetId",
                    coreAuth.userId.toString(),
                    jwtTokenComponent.jwtProperties.registerVerifyTokenExpiration
                )
            }
            val resetUrl = "https://${siteProperities.domain}/reset-password?token=" + URLEncoder.encode(token, "UTF-8")
            emailComponent.sendPasswordResetEmail(request.email, coreAuth.username ?: "", resetUrl)
        } else {
            authEnumerationProtectionService.simulateForgotPassword(request.email)
        }
        return ResponseBase("若该邮箱已注册，我们将发送一封密码重置邮件到该邮箱，请注意查收")
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest, ip: String): ResponseBase<String> {
        val resetId = jwtTokenComponent.getPasswordResetIdFromToken(request.token)
            ?: return ResponseBase(400, "无效的重置链接")
        val redisKey = "password-reset:$resetId"
        objectRedisTemplate.opsForValue().getAndDelete(redisKey)
            ?: return ResponseBase(400, "重置链接已失效或已被使用")
        if (!jwtTokenComponent.isJWTVaild(request.token)) {
            return ResponseBase(400, "重置链接已过期")
        }
        val email = jwtTokenComponent.getEmailFromToken(request.token)
            ?: return ResponseBase(400, "无效的重置链接")
        val coreAuth = coreAuthService.getByEmail(email)
            ?: return ResponseBase(400, "无效的重置链接")
        coreAuthService.updateById(coreAuth.apply {
            srpSalt = request.salt
            srpVerifier = request.verifier
        })
        userOperationService.log(coreAuth.userId, EnumUserOperation.PASSWORD_RESET, "通过邮箱重置密码", ip = ip)
        return ResponseBase("密码重置成功，您现在可以使用新密码登录了")
    }
}
