package ltd.guimc.web.altget.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.db.alt.AltConsumptionRecord
import ltd.guimc.web.altget.entity.db.coin.CoinToken
import ltd.guimc.web.altget.entity.db.user.UserOperation
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.PageResponse
import ltd.guimc.web.altget.entity.response.alt.AltConsumptionResponse
import ltd.guimc.web.altget.entity.response.user.AdminOperationLogResponse
import ltd.guimc.web.altget.entity.response.user.CoinTokenResponse
import ltd.guimc.web.altget.entity.response.user.SimpleUserInfo
import ltd.guimc.web.altget.entity.response.user.UserInfo
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.alt.AltConsumptionRecordService
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

@RestController
class AdminController(
    private val coreAuthService: CoreAuthService,
    private val userDetailsService: UserDetailsService,
    private val userApiService: UserApiService,
    private val userCoinService: UserCoinService,
    private val coinTokenService: CoinTokenService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService,
    private val userOperationService: UserOperationService,
    private val altConsumptionRecordService: AltConsumptionRecordService
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
        val userApi = userApiService.getById(userId) ?: return null
        return UserInfo(
            id = userId,
            name = coreAuth.username ?: "",
            email = coreAuth.email ?: "",
            role = userDetails.userRole,
            apiLimitLevel = userApi.limitLevel,
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
    fun listUsers(@CurrentUserId userId: Int?, @RequestParam size: Int = 20, @RequestParam page: Int = 1): ResponseBase<PageResponse<SimpleUserInfo>> {
        requireAdmin<PageResponse<SimpleUserInfo>>(userId)?.let { return it }
        val pageResult = coreAuthService.getPage(page, size)
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
    fun banUser(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int): ResponseBase<String> {
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
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_BAN, "Banned user $targetUserId")
        return ResponseBase("User banned successfully")
    }

    @PostMapping("/api/admin/user/{targetUserId}/unban")
    @Transactional
    fun unbanUser(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        val userDetails = userDetailsService.getById(targetUserId)
            ?: return ResponseBase(404, "User not found")
        if (userDetails.userRole != EnumUserRole.BANNED)
            return ResponseBase(400, "User is not banned")
        userDetailsService.updateById(userDetails.apply {
            userRole = EnumUserRole.VERIFY
        })
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_UNBAN, "Unbanned user $targetUserId")
        return ResponseBase("User unbanned successfully")
    }
    // </editor-fold>

    // <editor-fold desc="Add / Subtract Credit">
    @PostMapping("/api/admin/user/{targetUserId}/credit/add")
    @Transactional
    fun addCredit(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int, @RequestParam amount: Int): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        if (amount <= 0) return ResponseBase(400, "Amount must be positive")
        val userCoin = userCoinService.getById(targetUserId)
            ?: return ResponseBase(404, "User not found")
        userCoinService.updateById(userCoin.apply {
            balance += amount
        })
        coinTransactionHistoryService.logTransaction(
            userId = targetUserId,
            amount = amount,
            type = EnumTransactionType.ADMIN_ADD
        )
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_CREDIT_ADD, "Added $amount credits to user $targetUserId")
        return ResponseBase("Successfully added $amount credits to user $targetUserId")
    }

    @PostMapping("/api/admin/user/{targetUserId}/credit/subtract")
    @Transactional
    fun subtractCredit(@CurrentUserId userId: Int?, @PathVariable targetUserId: Int, @RequestParam amount: Int): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        if (amount <= 0) return ResponseBase(400, "Amount must be positive")
        val userCoin = userCoinService.getById(targetUserId)
            ?: return ResponseBase(404, "User not found")
        if (userCoin.balance < amount)
            return ResponseBase(400, "User does not have enough credits (balance: ${userCoin.balance})")
        userCoinService.updateById(userCoin.apply {
            balance -= amount
        })
        coinTransactionHistoryService.logTransaction(
            userId = targetUserId,
            amount = -amount,
            type = EnumTransactionType.ADMIN_SUBTRACT
        )
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_CREDIT_SUBTRACT, "Subtracted $amount credits from user $targetUserId")
        return ResponseBase("Successfully subtracted $amount credits from user $targetUserId")
    }
    // </editor-fold>

    // <editor-fold desc="Generate Tokens">
    @PostMapping("/api/admin/token/generate")
    @Transactional
    fun generateTokens(@CurrentUserId userId: Int?, @RequestParam tokenAmount: Int, @RequestParam coinAmount: Int): ResponseBase<List<String>> {
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
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_TOKEN_GENERATE, "Generated $tokenAmount tokens with $coinAmount coins each")
        return ResponseBase(tokens)
    }
    // </editor-fold>

    // <editor-fold desc="List Tokens">
    @GetMapping("/api/admin/tokens")
    fun listTokens(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseBase<PageResponse<CoinTokenResponse>> {
        requireAdmin<PageResponse<CoinTokenResponse>>(userId)?.let { return it }
        val pageResult = coinTokenService.getPage(page, size)
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
    fun removeToken(@CurrentUserId userId: Int?, @PathVariable tokenId: String): ResponseBase<String> {
        requireAdmin<String>(userId)?.let { return it }
        val token = coinTokenService.getById(tokenId)
            ?: return ResponseBase(404, "Token not found")
        coinTokenService.removeById(tokenId)
        userOperationService.log(userId!!, EnumUserOperation.ADMIN_TOKEN_REMOVE, "Removed token $tokenId (coinAmount=${token.coinAmount}, isUsed=${token.isUsed})")
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
                description = op.description ?: ""
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

    // <editor-fold desc="Alt Consumption Statistics">
    /**
     * Query alt consumption records for frontend chart rendering.
     * Minimum time granularity is one hour.
     *
     * @param channel  Optional filter by source channel
     * @param startTime Optional start of time range (ISO-8601, e.g. "2026-06-01T00:00:00")
     * @param endTime   Optional end of time range (ISO-8601, e.g. "2026-06-02T00:00:00")
     * @return List of hourly consumption records
     */
    @GetMapping("/api/admin/alt-consumption")
    fun getAltConsumption(
        @CurrentUserId userId: Int?,
        @RequestParam(required = false) channel: String?,
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?
    ): ResponseBase<List<AltConsumptionResponse>> {
        requireAdmin<List<AltConsumptionResponse>>(userId)?.let { return it }
        val queryWrapper = QueryWrapper<AltConsumptionRecord>()
            .apply { channel?.let { eq("channel", it) } }
            .apply { startTime?.let { ge("hour_slot", it) } }
            .apply { endTime?.let { le("hour_slot", it) } }
            .orderByAsc("hour_slot")
        val records = altConsumptionRecordService.list(queryWrapper)
        val response = records.map { record ->
            AltConsumptionResponse(
                channel = record.channel,
                hourSlot = record.hourSlot.toString(),
                consumedCount = record.consumedCount
            )
        }
        return ResponseBase(response)
    }
    // </editor-fold>
}