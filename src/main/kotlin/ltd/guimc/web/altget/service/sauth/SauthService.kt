package ltd.guimc.web.altget.service.sauth

import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONObject
import ltd.guimc.web.altget.config.SiteProperities
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SauthService(private val siteProperities: SiteProperities) {

    private val log = LoggerFactory.getLogger(SauthService::class.java)

    /**
     * 4399 账号登录
     *
     * @param username 4399 用户名
     * @param password 密码
     * @param proxy    代理地址 (可选，如 http://127.0.0.1:7890)
     * @return Map containing success, message, and optionally sauthJson
     */
    fun doLogin(username: String, password: String, proxy: String? = null): Map<String, Any?> {
        val url = siteProperities.sauthServiceBaseUrl.trimEnd('/') + "/login"

        try {
            val httpRequest = HttpUtil.createPost(url)
                .header("Content-Type", "application/json")
                .body(JSONObject().apply {
                    set("username", username)
                    set("password", password)
                    if (!proxy.isNullOrBlank()) {
                        set("proxy", proxy)
                    }
                }.toString())

            // If proxy is specified, set it on the request
            if (!proxy.isNullOrBlank()) {
                val afterProtocol = proxy.substringAfter("://")
                val host = afterProtocol.substringBefore(":")
                val port = afterProtocol.substringAfter(":", "80").toIntOrNull() ?: 80
                httpRequest.setHttpProxy(host, port)
            }

            val response = httpRequest.execute()
            val body = response.body()

            if (!response.isOk) {
                log.warn("Sauth service returned non-OK status: {} - {}", response.status, body)
                return mapOf(
                    "success" to false,
                    "message" to "登录服务暂时不可用，请稍后再试"
                )
            }

            val json = JSONObject(body)
            return mapOf(
                "success" to json.getBool("success", false),
                "message" to json.getStr("message", "未知错误"),
                "sauthJson" to json.getStr("sauth_json")
            )
        } catch (e: Exception) {
            log.error("Failed to call sauth login service", e)
            return mapOf(
                "success" to false,
                "message" to "登录服务暂时不可用，请稍后再试"
            )
        }
    }
}
