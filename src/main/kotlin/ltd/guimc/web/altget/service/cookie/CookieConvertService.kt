package ltd.guimc.web.altget.service.cookie

import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil
import ltd.guimc.web.altget.config.CookieConvertProperties
import org.springframework.stereotype.Service

@Service
class CookieConvertService(
    private val properties: CookieConvertProperties,
    private val billingService: CookieConvertBillingService,
) {
    fun convert(userId: Int, account: String, password: String): CookieConvertResult {
        billingService.charge(userId)

        val result = requestUpstream(account, password)
        if (!result.success) {
            billingService.refund(userId)
        }
        return result
    }

    private fun requestUpstream(account: String, password: String): CookieConvertResult {
        if (properties.upstreamUrl.isBlank()) return CookieConvertResult.failure(UNAVAILABLE_MESSAGE)

        val response = try {
            HttpUtil.createPost(properties.upstreamUrl)
                .header("Content-Type", "application/json")
                .timeout(properties.timeoutMillis)
                .body(JSONUtil.toJsonStr(mapOf("username" to account, "password" to password)))
                .execute()
        } catch (_: Exception) {
            return CookieConvertResult.failure(UNAVAILABLE_MESSAGE)
        }

        try {
            val upstream = CookieConvertUpstreamResponse.parse(response.body())
                ?: return CookieConvertResult.failure(UNAVAILABLE_MESSAGE)
            if (upstream.code != 0) {
                return CookieConvertResult.failure(safeUpstreamMessage(upstream.message))
            }
            if (!response.isOk) return CookieConvertResult.failure(UNAVAILABLE_MESSAGE)
            val cookie = upstream.cookie ?: return CookieConvertResult.failure(UNAVAILABLE_MESSAGE)
            return CookieConvertResult.success(cookie)
        } finally {
            response.close()
        }
    }

    private fun safeUpstreamMessage(message: String?): String =
        message?.trim()?.take(500)?.takeIf { it.isNotBlank() } ?: "转换失败"

    companion object {
        private const val UNAVAILABLE_MESSAGE = "转换服务暂时不可用，请稍后重试"
    }
}

class CookieConvertResult private constructor(
    val success: Boolean,
    val cookie: String?,
    val message: String?,
) {
    companion object {
        fun success(cookie: String) = CookieConvertResult(true, cookie, null)

        fun failure(message: String) = CookieConvertResult(false, null, message)
    }
}
