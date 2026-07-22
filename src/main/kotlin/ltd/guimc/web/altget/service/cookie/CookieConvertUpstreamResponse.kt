package ltd.guimc.web.altget.service.cookie

import cn.hutool.json.JSONUtil

data class CookieConvertUpstreamResponse(
    val code: Int,
    val message: String?,
    val cookie: String?,
) {
    companion object {
        fun parse(body: String): CookieConvertUpstreamResponse? = try {
            val json = JSONUtil.parseObj(body)
            val code = json.getInt("code") ?: return null
            CookieConvertUpstreamResponse(
                code = code,
                message = json.getStr("message"),
                cookie = json.getStr("data"),
            )
        } catch (_: Exception) {
            null
        }
    }
}
