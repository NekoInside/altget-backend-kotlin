package ltd.guimc.web.altget.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import jakarta.servlet.http.HttpServletResponse
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.entity.db.alt.UsedAltCategory
import ltd.guimc.web.altget.entity.db.user.UserApi
import ltd.guimc.web.altget.entity.request.user.ApiKeyRequest
import ltd.guimc.web.altget.entity.response.PageResponse
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.alt.UsedAltResponse
import ltd.guimc.web.altget.entity.response.user.UserApiKeyInfo
import ltd.guimc.web.altget.entity.response.user.UserInfo
import ltd.guimc.web.altget.entity.response.user.UserOperationLogResponse
import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.service.alt.UsedAltCategoryService
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.pow.PoWTaskService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.OutputStreamWriter
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.UUID

@RestController
class UserController(
    private val coreAuthService: CoreAuthService,
    private val userDetailsService: UserDetailsService,
    private val userApiService: UserApiService,
    private val userOperationService: UserOperationService,
    private val geetestVerifyComponent: GeetestVerifyComponent,
    private val poWTaskService: PoWTaskService,
    private val userCoinService: UserCoinService,
    private val usedAltCategoryService: UsedAltCategoryService,
    @Qualifier("objectRedisTemplate")
    private val objectRedisTemplate: RedisTemplate<String, Any>
) {
    // <editor-fold desc="Helper">

    /**
     * Returns null if the current user is authenticated, otherwise returns an error ResponseBase.
     */
    private fun <T> requireAuth(currentUserId: Int?): ResponseBase<T>? {
        if (currentUserId == null) return ResponseBase(401, "Unauthorized")
        return null
    }

    /**
     * Builds a [UserInfo] DTO from the composite data of [CoreAuth], [UserDetails], and [UserApi].
     */
    private fun buildUserInfo(userId: Int): UserInfo? {
        val coreAuth = coreAuthService.getById(userId) ?: return null
        val userDetails = userDetailsService.getById(userId) ?: return null
        val userApi = userApiService.getById(userId)
        val userCoin = userCoinService.getById(userId)
        return UserInfo(
            id = userId,
            name = coreAuth.username ?: "",
            email = coreAuth.email ?: "",
            role = userDetails.userRole,
            balance = userCoin?.balance ?: 0,
            apiLimitLevel = userApi?.limitLevel ?: EnumApiLimitLevel.LEVEL_LOW,
            registrationTime = userDetails.registerTime.toString(),
            lastLoginIp = userDetails.lastLoginIp,
            lastLoginGeo = userDetails.lastLoginGeo,
            lastLoginTime = userDetails.lastLoginTime.toString(),
            totalAltFetched = userDetails.totalAltFetched,
            dailyAltFetched = userDetails.dailyAltFetched,
            totalWebFetched = userDetails.totalWebFetched,
            dailyWebFetched = userDetails.dailyWebFetched,
            totalUserApiFetched = userDetails.totalUserApiFetched,
            dailyUserApiFetched = userDetails.dailyUserApiFetched,
            totalPaidApiFetched = userDetails.totalPaidApiFetched,
            dailyPaidApiFetched = userDetails.dailyPaidApiFetched
        )
    }

    private fun <T> verifyPoWAndCaptcha(request: ApiKeyRequest, target: String): ResponseBase<T>? {
        if (!geetestVerifyComponent.verify(request)) {
            return ResponseBase(400, "人机验证校验失败")
        }
        if (!poWTaskService.validateTask(request.taskId, target, request.result)) {
            return ResponseBase(400, "PoW 验证失败")
        }
        return null
    }
    // </editor-fold>

    // <editor-fold desc="Get Self Information">
    @GetMapping("/api/user/self")
    fun getSelf(@CurrentUserId userId: Int?): ResponseBase<UserInfo> {
        requireAuth<UserInfo>(userId)?.let { return it }
        val userInfo = buildUserInfo(userId!!)
            ?: return ResponseBase(500, "Failed to retrieve user information")
        return ResponseBase(userInfo)
    }
    // </editor-fold>

    // <editor-fold desc="Get Self Operation Log">
    @GetMapping("/api/user/self/operations")
    fun getSelfOperations(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseBase<List<UserOperationLogResponse>> {
        requireAuth<List<UserOperationLogResponse>>(userId)?.let { return it }
        val queryWrapper = com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ltd.guimc.web.altget.entity.db.user.UserOperation>()
            .eq("user_id", userId)
            .orderByDesc("operation_time")
        val pageResult = userOperationService.getPage(page, size, queryWrapper)
        val logs = pageResult.records.map { op ->
            UserOperationLogResponse(
                operationTime = op.operationTime.toString(),
                operation = op.operation ?: EnumUserOperation.LOGIN,
                description = op.description ?: ""
            )
        }
        return ResponseBase(logs)
    }
    // </editor-fold>

    // <editor-fold desc="Get Self API Key Info">
    @GetMapping("/api/user/self/api-key")
    fun getSelfApiKey(@CurrentUserId userId: Int?): ResponseBase<UserApiKeyInfo> {
        requireAuth<UserApiKeyInfo>(userId)?.let { return it }
        val userApi = userApiService.getById(userId!!)
            ?: return ResponseBase(404, "API key not found. Please create one first.")
        return ResponseBase(UserApiKeyInfo(
            apiKey = userApi.apiKey,
            limitLevel = userApi.limitLevel
        ))
    }
    // </editor-fold>

    // <editor-fold desc="New User API Key">
    @PostMapping("/api/user/self/api-key/new")
    @Transactional
    fun newApiKey(@CurrentUserId userId: Int?, @RequestBody request: ApiKeyRequest, @RealIP ip: String): ResponseBase<UserApiKeyInfo> {
        requireAuth<UserApiKeyInfo>(userId)?.let { return it }
        verifyPoWAndCaptcha<UserApiKeyInfo>(request, "new-api")?.let { return it }

        val existingApi = userApiService.getById(userId!!)
        val newApiKey = UUID.randomUUID().toString()
        if (existingApi != null) {
            userApiService.updateById(existingApi.apply {
                apiKey = newApiKey
            })
        } else {
            userApiService.save(UserApi().apply {
                this.userId = userId
                this.apiKey = newApiKey
            })
        }
        userOperationService.log(userId, EnumUserOperation.API_KEY_NEW, "Created new API key", ip = ip)
        val updatedApi = userApiService.getById(userId)
            ?: return ResponseBase(500, "Failed to create API key")
        return ResponseBase(UserApiKeyInfo(
            apiKey = updatedApi.apiKey,
            limitLevel = updatedApi.limitLevel
        ))
    }
    // </editor-fold>

    // <editor-fold desc="Rotate User API Key">
    @PostMapping("/api/user/self/api-key/rotate")
    @Transactional
    fun rotateApiKey(@CurrentUserId userId: Int?, @RequestBody request: ApiKeyRequest, @RealIP ip: String): ResponseBase<UserApiKeyInfo> {
        requireAuth<UserApiKeyInfo>(userId)?.let { return it }
        verifyPoWAndCaptcha<UserApiKeyInfo>(request, "new-api")?.let { return it }

        val existingApi = userApiService.getById(userId!!)
            ?: return ResponseBase(404, "API key not found. Please create one first.")
        val newApiKey = UUID.randomUUID().toString()
        userApiService.updateById(existingApi.apply {
            apiKey = newApiKey
        })
        userOperationService.log(userId, EnumUserOperation.API_KEY_ROTATE, "Rotated API key", ip = ip)
        val updatedApi = userApiService.getById(userId)
            ?: return ResponseBase(500, "Failed to rotate API key")
        return ResponseBase(UserApiKeyInfo(
            apiKey = updatedApi.apiKey,
            limitLevel = updatedApi.limitLevel
        ))
    }
    // </editor-fold>

    // <editor-fold desc="List Self Used Alts">
    @GetMapping("/api/user/self/used-alts")
    fun getSelfUsedAlts(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) channel: String?,
        @RequestParam(required = false) username: String?
    ): ResponseBase<PageResponse<UsedAltResponse>> {
        requireAuth<PageResponse<UsedAltResponse>>(userId)?.let { return it }
        if (page < 1 || size < 1 || size > 100) return ResponseBase(400, "Invalid pagination (page >= 1, 1 <= size <= 100)")
        val normalizedUsername = username?.trim()
        // 硬编码 user_id 过滤，用户只能查询自己获取过的小号
        val queryWrapper = QueryWrapper<UsedAltCategory>()
            .eq("user_id", userId)
            .apply { channel?.let { eq("channel", it) } }
            .apply { if (!normalizedUsername.isNullOrEmpty()) like("username", normalizedUsername) }
            .orderByDesc("fetch_time")
        val pageResult = usedAltCategoryService.getPage(page, size, queryWrapper)
        val records = pageResult.records.map { alt ->
            UsedAltResponse(
                id = alt.id,
                username = alt.username,
                password = alt.password,
                channel = alt.channel,
                userId = alt.userId,
                operationIp = alt.operationIp,
                fetchMethod = alt.fetchMethod,
                fetchTime = alt.fetchTime.toString(),
                createdAt = alt.createdAt?.toString()
            )
        }
        return ResponseBase(PageResponse(
            records = records,
            total = pageResult.total,
            size = pageResult.size,
            current = pageResult.current,
            pages = pageResult.pages
        ))
    }
    // </editor-fold>

    // <editor-fold desc="Export Self Used Alts as CSV">
    @GetMapping("/api/user/self/used-alts/export")
    fun exportSelfUsedAlts(
        @CurrentUserId userId: Int?,
        @RealIP ip: String,
        response: HttpServletResponse
    ): ResponseBase<String> {
        requireAuth<String>(userId)?.let { return it }

        // 限流：每 24 小时仅允许导出一次
        val cooldownKey = "used-alt:export:cooldown:$userId"
        if (objectRedisTemplate.hasKey(cooldownKey) == true) {
            return ResponseBase(400, "每天只能导出一次，请稍后再试")
        }

        val records = usedAltCategoryService.list(
            QueryWrapper<UsedAltCategory>()
                .eq("user_id", userId)
                .orderByDesc("fetch_time")
        )

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now())
        response.contentType = "text/csv; charset=UTF-8"
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"used-alts-$userId-$timestamp.csv\""
        )

        // UTF-8 BOM，确保 Excel 正确识别中文编码
        OutputStreamWriter(response.outputStream, Charsets.UTF_8).use { writer ->
            writer.write("\uFEFF")
            writer.write("ID,用户名,密码,渠道,获取方式,获取IP,获取时间,入库时间")
            writer.write("\r\n")
            for (alt in records) {
                writer.write(listOf(
                    alt.id?.toString(),
                    alt.username,
                    alt.password,
                    alt.channel,
                    alt.fetchMethod,
                    alt.operationIp,
                    alt.fetchTime.toString(),
                    alt.createdAt?.toString()
                ).joinToString(",") { csvEscape(it) })
                writer.write("\r\n")
            }
            writer.flush()
        }

        // 标记本次导出，24h 后方可再次导出
        objectRedisTemplate.opsForValue().set(cooldownKey, timestamp, Duration.ofHours(24))
        userOperationService.log(userId!!, EnumUserOperation.USED_ALT_EXPORT, "Exported ${records.size} used alt(s) as CSV", ip = ip)
        return ResponseBase(200, "导出成功，共 ${records.size} 条记录")
    }

    /**
     * RFC 4180 CSV 字段转义：字段为 null 时返回空串；
     * 字段包含逗号 / 双引号 / 换行时用双引号包裹，内部双引号转义为两个连续双引号。
     */
    private fun csvEscape(value: String?): String {
        val raw = value ?: ""
        if (raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return "\"" + raw.replace("\"", "\"\"") + "\""
        }
        return raw
    }
    // </editor-fold>
}
