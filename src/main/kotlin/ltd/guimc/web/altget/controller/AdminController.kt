package ltd.guimc.web.altget.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.entity.db.alt.AltConsumptionRecord
import ltd.guimc.web.altget.entity.db.alt.UsedAltCategory
import ltd.guimc.web.altget.entity.db.coin.CoinToken
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.entity.db.user.UserOperation
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.PageResponse
import ltd.guimc.web.altget.entity.response.alt.AltConsumptionResponse
import ltd.guimc.web.altget.entity.response.alt.UsedAltResponse
import ltd.guimc.web.altget.entity.response.user.AdminOperationLogResponse
import ltd.guimc.web.altget.entity.response.user.CoinTokenResponse
import ltd.guimc.web.altget.entity.response.user.SimpleUserInfo
import ltd.guimc.web.altget.entity.response.user.UserInfo
import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.alt.AltConsumptionRecordService
import ltd.guimc.web.altget.service.alt.UsedAltCategoryService
import ltd.guimc.web.altget.service.coin.CoinTokenService
import ltd.guimc.web.altget.service.coin.CoinTransactionHistoryService
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
class AdminController(
    private val coreAuthService: CoreAuthService,
    private val userDetailsService: UserDetailsService,
    private val userApiService: UserApiService,
    private val userCoinService: UserCoinService,
    private val coinTokenService: CoinTokenService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService,
    private val userOperationService: UserOperationService,
    private val altConsumptionRecordService: AltConsumptionRecordService,
    private val usedAltCategoryService: UsedAltCategoryService
) {
    // <editor-fold desc="Helper">
    /**
     * Returns null if the current user is an admin, otherwise returns an error ResponseBase.
     */
    private fun <T> requireAdmin(currentUserId: Int?): ResponseBase<T>? {
        if (currentUserId == null) return ResponseBase(401, "Unauthorized")
        val userDetails = userDetailsService.getById(currentUserId)
            ?: return ResponseBase(401, "Unauthorized")
        if (userDetails.userRole != EnumUserRole.ADMIN)
            return ResponseBase(403, "Forbidden: Admin role required")
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
            balance = userCoin?.balance ?: 0L,
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

    /**
     * Build a [SimpleUserInfo] DTO which only contains basic info for listing users, to optimize performance by avoiding unnecessary joins.
     */
    private fun buildSimpleUserInfo(userId: Int): SimpleUserInfo? {
        val coreAuth = coreAuthService.getById(userId) ?: return null
        val userDetails = userDetailsService.getById(userId) ?: return null
        return SimpleUserInfo(
            id = userId,
            name = coreAuth.username ?: "",
            email = coreAuth.email ?: "",
            role = userDetails.userRole,
        )
    }
    // </editor-fold>

    // <editor-fold desc="List Users">
    @GetMapping("/api/admin/users")
    fun listUsers(
        @CurrentUserId userId: Int?,
        @RequestParam size: Int = 20,
        @RequestParam page: Int = 1,
        @RequestParam(required = false) keyword: String?
    ): ResponseBase<PageResponse<SimpleUserInfo>> {
        requireAdmin<PageResponse<SimpleUserInfo>>(userId)?.let { return it }
        val normalizedKeyword = keyword?.trim()
        val queryWrapper = QueryWrapper<CoreAuth>().apply {
            if (!normalizedKeyword.isNullOrEmpty()) {
                nested { wrapper ->
                    normalizedKeyword.toIntOrNull()?.let { wrapper.eq("user_id", it).or() }
                    wrapper.like("username", normalizedKeyword)
                        .or()
                        .like("email", normalizedKeyword)
                }
            }
            orderByDesc("user_id")
        }
        val pageResult = coreAuthService.getPage(page, size, queryWrapper)
        val userInfoList = pageResult.records.mapNotNull { coreAuth ->
            buildSimpleUserInfo(coreAuth.userId)
        }
        return ResponseBase(PageResponse(
            records = userInfoList,
            total = pageResult.total,
            size = pageResult.size,
            current = pageResult.current,
            pages = pageResult.pages
        ))
    }
    // </editor-fold>

    // <editor-fold desc="Get User">
    @GetMapping("/api/admin/user/{targetUserId}")
    fun getUser(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int): ResponseBase<UserInfo> {
        requireAdmin<UserInfo>(userId)?.let { return it }
        val userInfo = buildUserInfo(targetUserId)
            ?: return ResponseBase(404, "User not found")
        return ResponseBase(userInfo)
    }
    // </editor-fold>

    // <editor-fold desc="Ban / Unban">
    @PostMapping("/api/admin/user/{targetUserId}/ban")
    @Transactional
    fun banUser(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int, @RealIP ip: String): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        val userDetails = userDetailsService.getById(targetUserId)
            ?: return ResponseBase(404, "User not found")
        if (userDetails.userRole == EnumUserRole.BANNED)
            return ResponseBase(400, "User is already banned")
        if (userDetails.userRole == EnumUserRole.ADMIN)
            return ResponseBase(400, "Cannot ban an admin user")
        userDetailsService.updateById(userDetails.apply {
            userRole = EnumUserRole.BANNED
        })
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_BAN, "Banned user $targetUserId", ip = ip)
        return ResponseBase("User banned successfully")
    }

    @PostMapping("/api/admin/user/{targetUserId}/unban")
    @Transactional
    fun unbanUser(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int, @RealIP ip: String): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        val userDetails = userDetailsService.getById(targetUserId)
            ?: return ResponseBase(404, "User not found")
        if (userDetails.userRole != EnumUserRole.BANNED)
            return ResponseBase(400, "User is not banned")
        userDetailsService.updateById(userDetails.apply {
            userRole = EnumUserRole.VERIFY
        })
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_UNBAN, "Unbanned user $targetUserId", ip = ip)
        return ResponseBase("User unbanned successfully")
    }
    // </editor-fold>

    // <editor-fold desc="Add / Subtract Credit">
    @PostMapping("/api/admin/user/{targetUserId}/credit/add")
    @Transactional
    fun addCredit(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int, @RequestParam amount: Int, @RealIP ip: String): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        if (amount <= 0) return ResponseBase(400, "Amount must be positive")
        if (coreAuthService.getById(targetUserId) == null) return ResponseBase(404, "User not found")
        val userCoin = userCoinService.getById(targetUserId)
        if (userCoin == null) {
            userCoinService.save(UserCoin().apply {
                this.userId = targetUserId
                this.balance = amount.toLong()
            })
        } else {
            check(userCoinService.addBalance(targetUserId, amount.toLong())) { "Failed to update user balance" }
        }
        coinTransactionHistoryService.logTransaction(
            userId = targetUserId,
            amount = amount,
            type = EnumTransactionType.ADMIN_ADD
        )
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_CREDIT_ADD, "Added $amount credits to user $targetUserId", ip = ip)
        return ResponseBase("Successfully added $amount credits to user $targetUserId")
    }

    @PostMapping("/api/admin/user/{targetUserId}/credit/subtract")
    @Transactional
    fun subtractCredit(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int, @RequestParam amount: Int, @RealIP ip: String): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        if (amount <= 0) return ResponseBase(400, "Amount must be positive")
        val userCoin = userCoinService.getById(targetUserId) ?: return ResponseBase(404, "User not found")
        if (!userCoinService.subtractBalance(targetUserId, amount.toLong()))
            return ResponseBase(400, "User does not have enough credits (balance: ${userCoin.balance})")
        coinTransactionHistoryService.logTransaction(
            userId = targetUserId,
            amount = -amount,
            type = EnumTransactionType.ADMIN_SUBTRACT
        )
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_CREDIT_SUBTRACT, "Subtracted $amount credits from user $targetUserId", ip = ip)
        return ResponseBase("Successfully subtracted $amount credits from user $targetUserId")
    }
    // </editor-fold>

    // <editor-fold desc="Generate Tokens">
    @PostMapping("/api/admin/token/generate")
    @Transactional
    fun generateTokens(@CurrentUserId userId: Int?, @RequestParam tokenAmount: Int, @RequestParam coinAmount: Int, @RealIP ip: String): ResponseBase<List<String>> {
        requireAdmin<List<String>>(userId)?.let { return it }
        if (tokenAmount <= 0) return ResponseBase(400, "Token amount must be positive")
        if (coinAmount <= 0) return ResponseBase(400, "Coin amount must be positive")
        val tokens = (1..tokenAmount).map {
            val token = CoinToken().apply {
                this.coinAmount = coinAmount
                this.createdBy = userId
                this.isUsed = false
            }
            coinTokenService.save(token)
            token.id
        }
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_TOKEN_GENERATE, "Generated $tokenAmount tokens with $coinAmount coins each", ip = ip)
        return ResponseBase(tokens)
    }
    // </editor-fold>

    // <editor-fold desc="List Tokens">
    @GetMapping("/api/admin/tokens")
    fun listTokens(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "") keyword: String,
    ): ResponseBase<PageResponse<CoinTokenResponse>> {
        requireAdmin<PageResponse<CoinTokenResponse>>(userId)?.let { return it }
        val queryWrapper = QueryWrapper<CoinToken>().apply {
            if (keyword.isNotEmpty()) {
                nested { wrapper ->
                    wrapper.like("id", keyword)
                }
            }
        }
        val pageResult = coinTokenService.getPage(page, size, queryWrapper)
        val tokenList = pageResult.records.map { token ->
            CoinTokenResponse(
                id = token.id,
                coinAmount = token.coinAmount,
                createdBy = token.createdBy,
                createdAt = token.createdAt?.toString(),
                redeemedBy = token.redeemedBy,
                redeemedAt = token.redeemedAt?.toString(),
                isUsed = token.isUsed
            )
        }
        return ResponseBase(PageResponse(
            records = tokenList,
            total = pageResult.total,
            size = pageResult.size,
            current = pageResult.current,
            pages = pageResult.pages
        ))
    }
    // </editor-fold>

    // <editor-fold desc="Remove Token">
    @PostMapping("/api/admin/token/{tokenId}/remove")
    @Transactional
    fun removeToken(@CurrentUserId userId: Int?, @PathVariable tokenId: String, @RealIP ip: String): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        val token = coinTokenService.getById(tokenId)
            ?: return ResponseBase(404, "Token not found")
        coinTokenService.removeById(tokenId)
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_TOKEN_REMOVE, "Removed token $tokenId (coinAmount=${token.coinAmount}, isUsed=${token.isUsed})", ip = ip)
        return ResponseBase("Token removed successfully")
    }
    // </editor-fold>

    // <editor-fold desc="List Operation Log">
    @GetMapping("/api/admin/operations")
    fun listOperations(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) filterUserId: Int?,
        @RequestParam(required = false) filterOperation: Int?,
        @RequestParam(required = false) filterTimeStart: String?,
        @RequestParam(required = false) filterTimeEnd: String?
    ): ResponseBase<PageResponse<AdminOperationLogResponse>> {
        requireAdmin<PageResponse<AdminOperationLogResponse>>(userId)?.let { return it }
        val queryWrapper = QueryWrapper<UserOperation>()
            .apply { filterUserId?.let { eq("user_id", it) } }
            .apply { filterOperation?.let { eq("operation", it) } }
            .apply { filterTimeStart?.let { ge("operation_time", it) } }
            .apply { filterTimeEnd?.let { le("operation_time", it) } }
            .orderByDesc("operation_time")
        val pageResult = userOperationService.getPage(page, size, queryWrapper)
        val logs = pageResult.records.map { op ->
            AdminOperationLogResponse(
                operationId = op.operationId ?: "",
                operationTime = op.operationTime.toString(),
                userId = op.userId,
                username = op.username ?: "",
                operation = op.operation ?: EnumUserOperation.LOGIN,
                description = op.description ?: "",
                ip = op.ip,
                geoip = op.geoip
            )
        }
        return ResponseBase(PageResponse(
            records = logs,
            total = pageResult.total,
            size = pageResult.size,
            current = pageResult.current,
            pages = pageResult.pages
        ))
    }
    // </editor-fold>

    // <editor-fold desc="List Used Alts">
    /**
     * Query consumed (used) alts with optional filters.
     *
     * @param filterIp       Optional filter by the requesting IP
     * @param filterUserId   Optional filter by the user who fetched the alt
     * @param filterChannel  Optional filter by the original alt pool channel
     * @param filterUsername Optional substring/equals filter by alt username
     */
    @GetMapping("/api/admin/used-alts")
    fun listUsedAlts(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) filterIp: String?,
        @RequestParam(required = false) filterUserId: Int?,
        @RequestParam(required = false) filterChannel: String?,
        @RequestParam(required = false) filterUsername: String?
    ): ResponseBase<PageResponse<UsedAltResponse>> {
        requireAdmin<PageResponse<UsedAltResponse>>(userId)?.let { return it }
        val normalizedUsername = filterUsername?.trim()
        val queryWrapper = QueryWrapper<UsedAltCategory>()
            .apply { filterIp?.let { eq("operation_ip", it) } }
            .apply { filterUserId?.let { eq("user_id", it) } }
            .apply { filterChannel?.let { eq("channel", it) } }
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

    // <editor-fold desc="Alt Consumption Statistics">
    /**
     * Query alt fetch events for frontend chart rendering, bucketed by time.
     *
     * @param unit      Time bucket granularity: "1s", "1min", "1hour", or "1day" (default "1hour")
     * @param channel   Optional filter by alt pool channel (e.g. "default", "pre-processed")
     * @param startTime Optional start of time range (ISO-8601, e.g. "2026-07-07T00:00:00Z")
     * @param endTime   Optional end of time range (ISO-8601)
     * @return Bucketed consumption series per fetch source plus a "total" series
     */
    @GetMapping("/api/admin/alt-consumption")
    fun getAltConsumption(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1hour") unit: String,
        @RequestParam(required = false) channel: String?,
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?
    ): ResponseBase<AltConsumptionResponse> {
        requireAdmin<AltConsumptionResponse>(userId)?.let { return it }
        val chronoUnit = when (unit) {
            "1s" -> ChronoUnit.SECONDS
            "1min" -> ChronoUnit.MINUTES
            "1hour" -> ChronoUnit.HOURS
            "1day" -> ChronoUnit.DAYS
            else -> return ResponseBase(400, "Invalid unit: $unit (expected one of: 1s, 1min, 1hour, 1day)")
        }

        // Times are stored as LocalDateTime in the server timezone (UTC+8),
        // since LocalDateTime.now() uses the JVM default zone. Do all bucket
        // math in that same zone so the grid lines up with the stored values.
        val now = Instant.now().atZone(SERVER_ZONE).toLocalDateTime()
        val start = startTime?.let { parseServerLocal(it) }
            ?: now.truncatedTo(chronoUnit).minus((DEFAULT_RANGE_BUCKETS - 1).toLong(), chronoUnit)
        val end = endTime?.let { parseServerLocal(it) } ?: now

        val queryWrapper = QueryWrapper<AltConsumptionRecord>()
            .apply { channel?.let { eq("channel", it) } }
            .ge("event_time", start)
            .le("event_time", end)
        val events = altConsumptionRecordService.list(queryWrapper)

        // Build the ordered list of bucket starts in [start, end].
        val buckets = ArrayList<LocalDateTime>()
        var cursor = start.truncatedTo(chronoUnit)
        while (!cursor.isAfter(end)) {
            buckets.add(cursor)
            cursor = cursor.plus(1, chronoUnit)
        }

        // Sum counts into each bucket, per source.
        val sourceKeys = listOf("webfetch", "freeapifetch", "paidapifetch")
        val sums = HashMap<String, IntArray>(sourceKeys.size + 1)
        sourceKeys.forEach { sums[it] = IntArray(buckets.size) }
        val total = IntArray(buckets.size)
        val firstBucket = buckets.firstOrNull()
        for (event in events) {
            if (firstBucket == null) continue
            val bucketIndex = chronoUnit.between(firstBucket, event.time.truncatedTo(chronoUnit)).toInt()
            if (bucketIndex < 0 || bucketIndex >= buckets.size) continue
            val series = sums[event.source]
            if (series != null) series[bucketIndex] += event.count
            total[bucketIndex] += event.count
        }

        val timeLabels = buckets.map { it.toInstant(SERVER_ZONE).toString() }
        val values = LinkedHashMap<String, List<Int>>().apply {
            sourceKeys.forEach { put(it, sums[it]!!.toList()) }
            put("total", total.toList())
        }
        return ResponseBase(AltConsumptionResponse(time = timeLabels, values = values))
    }

    private fun parseServerLocal(iso: String): LocalDateTime =
        OffsetDateTime.parse(iso).withOffsetSameInstant(SERVER_ZONE).toLocalDateTime()

    private companion object {
        /** Number of buckets returned when no explicit time range is given. */
        private const val DEFAULT_RANGE_BUCKETS = 24

        /** Server timezone: stored LocalDateTime values are UTC+8 wall-clock. */
        private val SERVER_ZONE: ZoneOffset = ZoneOffset.ofHours(8)
    }
    // </editor-fold>
}
