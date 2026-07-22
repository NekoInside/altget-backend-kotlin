package ltd.guimc.web.altget.controller

import jakarta.servlet.http.HttpServletRequest
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.entity.request.cookie.CookieConvertRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.service.cookie.CookieConvertBalanceException
import ltd.guimc.web.altget.service.cookie.CookieConvertService
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CookieConvertController(
    private val cookieConvertService: CookieConvertService,
    private val userApiService: UserApiService,
    private val userOperationService: UserOperationService,
) {
    @PostMapping(
        "/api/alt/convert/cookie",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun convert(
        @CurrentUserId userId: Int?,
        @RequestBody request: CookieConvertRequest,
        @RealIP ip: String,
        requests: HttpServletRequest,
    ): ResponseBase<String> {
        if (request.account.isBlank() || request.password.isBlank()) {
            return ResponseBase(400, "账号和密码不能为空")
        }
        val resolvedUserId = requests.getHeader("Authorization")?.let { authorizationHeader ->
            val userApiKey = authorizationHeader.removePrefix("Ciallo ").trim()
            try {
                userApiService.getByApiKey(userApiKey).userId
            } catch (_: Exception) {
                return ResponseBase(400, "无效的 API Key")
            }
        } ?: userId ?: return ResponseBase(401, "请先登录或提供 API Key")

        return try {
            val result = cookieConvertService.convert(resolvedUserId, request.account, request.password)
            if (result.success) {
                userOperationService.log(resolvedUserId, EnumUserOperation.COOKIE_CONVERT, "Cookie conversion completed", ip)
                ResponseBase(result.cookie!!)
            } else {
                userOperationService.log(resolvedUserId, EnumUserOperation.COOKIE_CONVERT, "Cookie conversion failed", ip)
                ResponseBase(400, result.message ?: "转换失败")
            }
        } catch (_: CookieConvertBalanceException) {
            ResponseBase(400, "余额不足")
        } catch (_: Exception) {
            ResponseBase(500, "转换服务暂时不可用，请稍后重试")
        }
    }
}
