package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.component.DiscordApiService
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.entity.request.CaptchaRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.service.alt.AltService
import ltd.guimc.web.altget.service.pow.PoWTaskService
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController("/api/alt")
class AltController(
    private val geetestVerifyComponent: GeetestVerifyComponent,
    private val poWTaskService: PoWTaskService,
    private val altService: AltService,
    private val userDetailsService: UserDetailsService,
    private val discordApiService: DiscordApiService
) {
    @GetMapping(params = ["userApiKey"])
    fun apiFetch(@RequestParam(required = false) userApiKey: String?, paid: Boolean = false, count: Int = 1): ResponseBase<List<String>> {
        // TODO
        return ResponseBase(arrayListOf())
    }

    @GetMapping(params = ["!userApiKey"])
    fun webFetch(captchaId: String, captchaOutput: String, genTime: String, lotNumber: String, passToken: String, // captcha
                 taskId: String, nonce: String, // PoW
                 channel: String = "default",
                 @CurrentUserId userId: Int?): ResponseBase<String> {
        val captchaRequest = CaptchaRequest(captchaId, captchaOutput, genTime, lotNumber, passToken)
        if (!geetestVerifyComponent.verify(captchaRequest)) {
            return ResponseBase(400, "Captcha verification failed")
        }
        if (!poWTaskService.validateTask(taskId, nonce)) {
            return ResponseBase(400, "PoW validation failed")
        }
        if (!meetChannelRequirement(channel, userId)) {
            return ResponseBase(403, "Channel requirement not met")
        }
        val data = altService.fetchAlt(1, channel)
        if (data.isEmpty()) {
            return ResponseBase(500, "No alt available")
        }
        val alt = data[0]
        return ResponseBase("${alt.username}----${alt.password}")
    }

    fun meetChannelRequirement(channel: String, userId: Int?): Boolean {
        if (channel == "pre-processed") {
            if (userId == null) return false
            val userDetails = userDetailsService.getById(userId) ?: return false
            val registerTime = userDetails.registerTime ?: LocalDateTime.now()
            val wantedRegisterTime = LocalDateTime.of(2026, 2, 23, 0, 0)
            return registerTime <= wantedRegisterTime || discordApiService.checkIsUserSponsor(userId)
        }
        return true
    }
}