package ltd.guimc.web.altget.controller

import jakarta.servlet.http.HttpServletRequest
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.component.DiscordApiService
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.entity.request.CaptchaRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.sauth.SauthGenerateResponse
import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.service.alt.AltService
import ltd.guimc.web.altget.service.alt.PayForAltService
import ltd.guimc.web.altget.service.geolocation.GeolocationService
import ltd.guimc.web.altget.service.pow.PoWTaskService
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class AltController(
    private val geetestVerifyComponent: GeetestVerifyComponent,
    private val poWTaskService: PoWTaskService,
    private val altService: AltService,
    private val altConsumptionRecordService: ltd.guimc.web.altget.service.alt.AltConsumptionRecordService,
    private val userDetailsService: UserDetailsService,
    private val discordApiService: DiscordApiService,
    private val userApiService: UserApiService,
    private val payForAltService: PayForAltService,
    private val userOperationService: UserOperationService,
    private val geolocationService: GeolocationService,
    private val sauthService: ltd.guimc.web.altget.service.sauth.SauthService
) {
    @GetMapping("/api/alt", headers = ["Authorization"])
    fun apiFetch(paid: Boolean = false, count: Int = 1, @RealIP ip: String, requests: HttpServletRequest): ResponseBase<List<String>> {
        val authorizationHeader = requests.getHeader("Authorization") ?: return ResponseBase(400, "Missing api key")
        val userApiKey = authorizationHeader.removePrefix("Ciallo ").trim()
        if (!paid && count != 1) return ResponseBase(400, "Not allowed")
        if (count <= 0) return ResponseBase(400, "Invalid count wanted")
        if (count > 1000) return ResponseBase(400, "Count too large")
        val userApi = try {
            userApiService.getByApiKey(userApiKey)
        } catch (_: Exception) {
            return ResponseBase(400, "Invalid api key")
        }
        val userDetail = userDetailsService.getById(userApi.userId)
        if (userDetail.userRole == EnumUserRole.BANNED) return ResponseBase(400, "User banned")
        if (paid) {
            try {
                val data = payForAltService.payForAltAs(count, userApi.userId, ip)
                val result = mutableListOf<String>()
                data.forEach { alt ->
                    val username = alt.username ?: ""
                    val password = alt.password ?: ""
                    result += "$username----$password"
                }
                val logMsg = "Fetched $count alts via paid API"
                userOperationService.log(userApi.userId, EnumUserOperation.PAID_API_FETCH, logMsg, ip = ip)
                userDetailsService.updateById(userDetail.apply {
                    totalPaidApiFetched += count
                    dailyPaidApiFetched += count
                    dailyAltFetched += count
                    totalAltFetched += count
                })
                return ResponseBase(result)
            } catch (e: RuntimeException) {
                return ResponseBase(400, e.message ?: "Error occurred")
            }
        }
        if (userDetail.dailyUserApiFetched >= userApi.limitLevel.limitDau &&
            userApi.limitLevel != EnumApiLimitLevel.LEVEL_UNLIMITED) return ResponseBase(400, "Daily limit reached")
        val data = altService.fetchAlt(count, "default", fetchMethod = "freeapi", userId = userApi.userId, ip = ip)
        if (data.isEmpty()) return ResponseBase(400, "No alt available")
        altConsumptionRecordService.recordFetch("freeapifetch", "default", userApi.userId, data.size)
        if (userDetail.dailyUserApiFetched++ >= userApi.limitLevel.limitDau &&
            userApi.limitLevel != EnumApiLimitLevel.LEVEL_UNLIMITED) {
            userApiService.updateById(userApi.apply {
                limitLevel = limitLevel.getHigherLimitLevel()
            })
        }
        userOperationService.log(userApi.userId, EnumUserOperation.API_FETCH, "Fetched alt via API", ip = ip)
        userDetailsService.updateById(userDetail.apply {
            dailyUserApiFetched += 1
            totalUserApiFetched += 1
            dailyAltFetched += 1
            totalAltFetched += 1
        })
        return ResponseBase(mutableListOf("${data[0].username}----${data[0].password}"))
    }

    @GetMapping("/api/alt", headers = ["!Authorization"])
    fun webFetch(captchaId: String, captchaOutput: String, genTime: String, lotNumber: String, passToken: String, // captcha
                 taskId: String, nonce: String, // PoW
                 channel: String = "default",
                 @CurrentUserId userId: Int?, @RealIP ip: String): ResponseBase<String> {
        val captchaRequest = CaptchaRequest(captchaId, captchaOutput, genTime, lotNumber, passToken)
        if (!geetestVerifyComponent.verify(captchaRequest)) {
            return ResponseBase(400, "Captcha verification failed")
        }
        if (!poWTaskService.validateTask(taskId, "fetch", nonce)) {
            return ResponseBase(400, "PoW validation failed")
        }
        if (!meetChannelRequirement(channel, userId)) {
            return ResponseBase(403, "Channel requirement not met")
        }
        if (userId == null && !meetAnonymousRequirement(ip)) {
            return ResponseBase(403, "Login required for your region")
        }
        val data = altService.fetchAlt(1, channel, fetchMethod = "web", userId = userId, ip = ip)
        if (data.isEmpty()) {
            return ResponseBase(500, "No alt available")
        }
        val alt = data[0]
        altConsumptionRecordService.recordFetch("webfetch", channel, userId, data.size)
        if (userId != null) {
            userOperationService.log(userId, EnumUserOperation.WEB_FETCH, "Fetched alt ${alt.username}----${alt.password} via web channel '$channel'", ip = ip)
            userDetailsService.updateById(userDetailsService.getById(userId)?.apply {
                dailyWebFetched += 1
                totalWebFetched += 1
                dailyAltFetched += 1
                totalAltFetched += 1
            })
        } else {
            userOperationService.logAnonymous(EnumUserOperation.WEB_FETCH, "Fetched alt ${alt.username}----${alt.password} via web channel '$channel'", ip = ip)
        }
        return ResponseBase("${alt.username}----${alt.password}")
    }

    fun meetChannelRequirement(channel: String, userId: Int?): Boolean {
        if (channel == "pre-processed") {
            if (userId == null) return false
            val userDetails = userDetailsService.getById(userId) ?: return false
            val registerTime = userDetails.registerTime
            val wantedRegisterTime = LocalDateTime.of(2026, 2, 23, 0, 0)
            return registerTime <= wantedRegisterTime || discordApiService.checkIsUserSponsor(userId)
        }
        return true
    }

    fun meetAnonymousRequirement(ip: String): Boolean {
        return geolocationService.getGeolocation(ip).countryName == "China"
    }

    @GetMapping("/api/alt/convert/gen")
    fun generateSauth(@RealIP ip: String, requests: HttpServletRequest): ResponseBase<SauthGenerateResponse> {
        val authorizationHeader = requests.getHeader("Authorization") ?: return ResponseBase(400, "Missing api key")
        val userApiKey = authorizationHeader.removePrefix("Ciallo ").trim()
        val userApi = try {
            userApiService.getByApiKey(userApiKey)
        } catch (_: Exception) {
            return ResponseBase(400, "Invalid api key")
        }
        altService.deductCoinForSauth(userApi.userId)
        try {
            val data = payForAltService.payForAltAs(1, userApi.userId, ip)
            if (data.isEmpty()) throw RuntimeException("No alt available")
            val logMsg = "Fetched 1 alt with sauth via paid API"
            userOperationService.log(userApi.userId, EnumUserOperation.PAID_API_FETCH, logMsg, ip = ip)
            val sauth = sauthService.doLogin(data[0].username!!, data[0].password!!)
            if (sauth["success"] as? Boolean != true) {
                throw RuntimeException(sauth["message"] as? String ?: "Sauth service error")
            }
            return ResponseBase(SauthGenerateResponse(data[0].username!!, data[0].password!!, sauth["sauthJson"] as? String ?: ""))
        } catch (e: RuntimeException) {
            altService.deductCoinForSauth(userApi.userId)
            return ResponseBase(400, e.message ?: "Error occurred")
        }
    }

    @GetMapping("/api/alt/convert/sauth")
    fun convertSauth(@CurrentUserId userId: Int?,
                     username: String, password: String, @RealIP ip: String, requests: HttpServletRequest): ResponseBase<String> {
        // Resolve userId from session or API key
        val authorizationHeader: String? = requests.getHeader("Authorization")
        val userApiKey = authorizationHeader?.removePrefix("Ciallo ")?.trim()
        val resolvedUserId: Int = if (!userApiKey.isNullOrBlank()) {
            try {
                userApiService.getByApiKey(userApiKey).userId
            } catch (_: Exception) {
                return ResponseBase(400, "无效的 API Key")
            }
        } else userId ?: return ResponseBase(401, "请先登录或提供 API Key")

        return try {
            val result = altService.convertAltToSauth(resolvedUserId, username, password)
            val success = result["success"] as? Boolean ?: false
            val message = result["message"] as? String ?: "未知错误"
            val sauthJson = result["sauthJson"] as? String
            userOperationService.log(resolvedUserId, EnumUserOperation.API_FETCH, "Sauth convert: $message", ip = ip)
            if (success) {
                ResponseBase(sauthJson ?: message)
            } else {
                ResponseBase(400, message)
            }
        } catch (e: RuntimeException) {
            ResponseBase(400, e.message ?: "操作失败")
        } catch (_: Exception) {
            ResponseBase(500, "服务器内部错误")
        }
    }
}