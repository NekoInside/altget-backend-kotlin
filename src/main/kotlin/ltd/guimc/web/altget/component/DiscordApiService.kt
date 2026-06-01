package ltd.guimc.web.altget.component

import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONObject
import ltd.guimc.web.altget.config.SiteProperities
import org.springframework.stereotype.Service

@Service
class DiscordApiService(private val siteProperities: SiteProperities) {
    fun getAccessTokenByOAuthCode(code: String): String {
        val redirectUrl = "https://${siteProperities.domain}/discord-callback"
        if (code.isBlank()) {
            throw IllegalArgumentException("Code cannot be blank")
        }
        val tokenResponse = HttpUtil.createPost("https://discord.com/api/oauth2/token")
            .form("grant_type", "authorization_code")
            .form("code", code)
            .form("redirect_uri", redirectUrl)
            .basicAuth(siteProperities.discordClientId, siteProperities.discordClientSecret)
            .execute()
        if (!tokenResponse.isOk) {
            throw RuntimeException("Failed to get access token: ${tokenResponse.body()}")
        }
        val respJson = JSONObject(tokenResponse.body())
        val accessToken = respJson.getStr("access_token")
        if (accessToken.isNullOrBlank()) {
            throw RuntimeException("Access token is blank in response: ${tokenResponse.body()}")
        }
        return accessToken
    }

    fun getUserInfoByAccessToken(accessToken: String): JSONObject? {
        if (accessToken.isBlank()) {
            throw IllegalArgumentException("Access token cannot be blank")
        }
        val userResponse = HttpUtil.createGet("https://discord.com/api/users/@me")
            .header("Authorization", "Bearer $accessToken")
            .execute()
        if (!userResponse.isOk) {
            throw RuntimeException("Failed to get user info: ${userResponse.body()}")
        }
        return JSONObject(userResponse.body())
    }
}