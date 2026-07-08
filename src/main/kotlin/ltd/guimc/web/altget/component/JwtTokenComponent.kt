package ltd.guimc.web.altget.component

import cn.hutool.core.date.DateUtil
import cn.hutool.jwt.JWT
import cn.hutool.jwt.JWTPayload
import cn.hutool.jwt.JWTUtil
import cn.hutool.jwt.JWTValidator
import cn.hutool.jwt.signers.HMacJWTSigner
import cn.hutool.jwt.signers.JWTSignerUtil
import ltd.guimc.web.altget.config.JwtProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtTokenComponent(
    val jwtProperties: JwtProperties,
    val redisSessionComponent: RedisSessionComponent
) {
    private val log = LoggerFactory.getLogger(JwtTokenComponent::class.java)

    // 半个有状态的 Session 信息保存
    fun generateLoginSession(userId: Int): String {
        var sessionId: String? = redisSessionComponent.getSession(userId)
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            redisSessionComponent.saveSession(userId, sessionId)
        }
        // get now date
        val now = Date()
        val jwtToken = JWT.create()
            .setKey(jwtProperties.secret.toByteArray())
            .setSubject(userId.toString())
            .setAudience(sessionId)
            .setIssuedAt(now)
            .setExpiresAt(Date(now.time + jwtProperties.accessTokenExpiration.toMillis()))
            .sign()
        return jwtToken
    }

    // 想偷懒所以直接写无状态验证邮箱了
    fun generateRegisterVerifyToken(email: String): String {
        val now = Date()
        val jwtToken = JWT.create()
            .setKey(jwtProperties.secret.toByteArray())
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiresAt(Date(now.time + jwtProperties.registerVerifyTokenExpiration.toMillis()))
            .sign()
        return jwtToken
    }

    // 忘记密码验证 token（有状态：嵌入唯一 JWT ID 用于防重放）
    fun generatePasswordResetToken(email: String): String {
        val now = Date()
        val resetId = UUID.randomUUID().toString()
        val jwtToken = JWT.create()
            .setKey(jwtProperties.secret.toByteArray())
            .setSubject(email)
            .setJWTId(resetId)
            .setIssuedAt(now)
            .setExpiresAt(Date(now.time + jwtProperties.registerVerifyTokenExpiration.toMillis()))
            .sign()
        return jwtToken
    }

    // 从密码重置 token 中提取唯一重置 ID（用于防重放检查）
    fun getPasswordResetIdFromToken(token: String): String? {
        try {
            if (!isJWTVaild(token)) {
                log.debug("Rejected password reset token because JWT validation failed")
                return null
            }
            val jwt = JWTUtil.parseToken(token)
            return jwt.getPayload(JWTPayload.JWT_ID) as? String
        } catch (exception: Exception) {
            log.debug("Failed to parse password reset token", exception)
        }
        return null
    }

    // 验证 JWT 是否过期 / 是否用正确的 Key 签名
    fun isJWTVaild(token: String): Boolean {
        try {
            JWTValidator.of(token)
                .validateDate(DateUtil.date())
                .validateAlgorithm(JWTSignerUtil.hs256(jwtProperties.secret.toByteArray()))
            return true
        } catch (exception: Exception) {
            log.debug("JWT validation failed", exception)
        }
        return false
    }

    fun getUserIdFromToken(token: String): Int? {
        try {
            if (!isJWTVaild(token)) {
                return null
            }
            val jwt = JWTUtil.parseToken(token)
            val userId = jwt.getPayload(JWTPayload.SUBJECT) as? String ?: return null
            val jwtSessionIdList = jwt.getPayload(JWTPayload.AUDIENCE) as? List<String> ?: return null
            val jwtSessionId = jwtSessionIdList.firstOrNull() ?: return null
            val userSessionId = redisSessionComponent.getSession(userId.toInt()) ?: return null
            if (jwtSessionId != userSessionId) {
                log.debug("Rejected JWT for user {} because session id does not match", userId)
                return null
            }
            return userId.toInt()
        } catch (exception: Exception) {
            log.debug("Failed to extract user id from JWT", exception)
        }
        return null
    }

    fun getEmailFromToken(token: String): String? {
        try {
            if (!isJWTVaild(token)) {
                return null
            }
            val jwt = JWTUtil.parseToken(token)
            val email = jwt.getPayload(JWTPayload.SUBJECT) as? String ?: return null
            return email
        } catch (exception: Exception) {
            log.debug("Failed to extract email from JWT", exception)
        }
        return null
    }
}