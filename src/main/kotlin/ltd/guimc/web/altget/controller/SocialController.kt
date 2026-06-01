package ltd.guimc.web.altget.controller

import cn.hutool.core.net.url.UrlBuilder
import jakarta.servlet.http.HttpServletResponse
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.component.DiscordApiService
import ltd.guimc.web.altget.config.SiteProperities
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.service.user.UserOauthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder

@RestController("/api/social")
class SocialController(
    private val siteProperities: SiteProperities,
    private val userOauthService: UserOauthService,
    private val discordApiService: DiscordApiService
) {
    // <editor-fold desc="Discord">
    @GetMapping("/discord/redirect")
    fun discordRedirect(response: HttpServletResponse): ResponseBase<String> {
        val encodedRedirectUri = URLEncoder.encode("https://${siteProperities.domain}/discord-callback")
        val url = UrlBuilder.of("https://discord.com/oauth2/authorize")
            .addQuery("client_id", siteProperities.discordClientId)
            .addQuery("redirect_uri", encodedRedirectUri)
            .addQuery("response_type", "code")
            .addQuery("scope", "identify+email")
            .build()
        response.sendRedirect(url)
        return ResponseBase("success")
    }

    @GetMapping("/discord/token")
    fun diccordBind(@CurrentUserId userId: Int?, code: String): ResponseBase<String> {
        if (userId == null) {
            return ResponseBase(404, "Not authenticated")
        }
        val userOauth = userOauthService.getById(userId)
        if (userOauth != null && !userOauth.discordId.isNullOrBlank()) {
            return ResponseBase(400, "Already bound to a Discord account")
        }
        if (code.isNullOrBlank()) {
            return ResponseBase(404, "Not authenticated")
        }
        try {
            val accessToken = discordApiService.getAccessTokenByOAuthCode(code)
            val discordUserInfo = discordApiService.getUserInfoByAccessToken(accessToken)
            val discordUserId = discordUserInfo?.getStr("id")
            if (discordUserId.isNullOrBlank()) {
                return ResponseBase(404, "Not authenticated")
            }
            if (userOauthService.getUserIdByDiscordId(discordUserId) != null) {
                return ResponseBase(400, "This Discord account is already bound to another user")
            }
            userOauthService.setDiscordId(userId, discordUserId)
            return ResponseBase(200, "OK")
        } catch (e: Exception) {
            return ResponseBase(500, "Failed to bind Discord account: ${e.message}")
        }
        return ResponseBase("")
    }
    // </editor-fold>
}