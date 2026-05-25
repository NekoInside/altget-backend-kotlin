package ltd.guimc.web.altget.component

import cn.hutool.jwt.JWT
import cn.hutool.jwt.JWTPayload
import cn.hutool.jwt.JWTUtil
import ltd.guimc.web.altget.config.JwtProperties
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtTokenComponent(
    val jwtProperties: JwtProperties,
    val redisSessionComponent: RedisSessionComponent
) {
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

    // 验证 JWT 是否过期 / 是否用正确的 Key 签名
    fun isJWTVaild(token: String): Boolean {
        try {
            if (!JWTUtil.verify(token, jwtProperties.secret.toByteArray())) {
                return false
            }
            val jwt = JWTUtil.parseToken(token)
            val jwtExpires = jwt.getPayload(JWTPayload.EXPIRES_AT) as? Date ?: return false
            if (jwtExpires.time < System.currentTimeMillis()) {
                return false
            }
            return true
        } catch (_: Exception) {
            // token 无效或过期
        }
        return false
    }

    fun getUserIdFromToken(token: String): Int? {
        try {
            if (!JWTUtil.verify(token, jwtProperties.secret.toByteArray())) {
                return null
            }
            val jwt = JWTUtil.parseToken(token)
            val userId = jwt.getPayload(JWTPayload.SUBJECT) as? String ?: return null
            val jwtSessionId = jwt.getPayload(JWTPayload.AUDIENCE) as? String ?: return null
            val userSessionId = redisSessionComponent.getSession(userId.toInt()) ?: return null
            if (jwtSessionId != userSessionId) {
                return null
            }
            return userId.toInt()
        } catch (_: Exception) {
            // 解析 JWT 失败?
        }
        return null
    }

    fun getEmailFromToken(token: String): String? {
        try {
            if (!JWTUtil.verify(token, jwtProperties.secret.toByteArray())) {
                return null
            }
            val jwt = JWTUtil.parseToken(token)
            val email = jwt.getPayload(JWTPayload.SUBJECT) as? String ?: return null
            return email
        } catch (_: Exception) {
            // 解析 JWT 失败?
        }
        return null
    }
}