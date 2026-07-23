package ltd.guimc.web.altget.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import ltd.guimc.web.altget.component.AdminBadRequestException
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.entity.db.alt.AltConsumptionRecord
import ltd.guimc.web.altget.entity.db.alt.UsedAltCategory
import ltd.guimc.web.altget.entity.db.coin.CoinToken
import ltd.guimc.web.altget.entity.db.coin.OxaPayRechargeOrder
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.entity.db.user.UserOperation
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.PageResponse
import ltd.guimc.web.altget.entity.response.alt.AltConsumptionResponse
import ltd.guimc.web.altget.entity.response.alt.UsedAltResponse
import ltd.guimc.web.altget.entity.response.admin.AdminDashboardSummaryResponse
import ltd.guimc.web.altget.entity.response.admin.AdminProfileResponse
import ltd.guimc.web.altget.entity.response.coin.AdminOxaPayRechargeResponse
import ltd.guimc.web.altget.entity.response.user.AdminOperationLogResponse
import ltd.guimc.web.altget.entity.response.user.CoinTokenResponse
import ltd.guimc.web.altget.entity.response.user.SimpleUserInfo
import ltd.guimc.web.altget.entity.response.user.UserInfo
import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import ltd.guimc.web.altget.enum.EnumOxaPayRechargeStatus
import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.alt.AltConsumptionRecordService
import ltd.guimc.web.altget.service.alt.UsedAltCategoryService
import ltd.guimc.web.altget.service.admin.AdminCsvExportService
import ltd.guimc.web.altget.service.admin.AdminList
import ltd.guimc.web.altget.service.admin.AdminSort
import ltd.guimc.web.altget.service.admin.AdminSortSpec
import ltd.guimc.web.altget.service.admin.CurrentAdminService
import ltd.guimc.web.altget.service.coin.CoinTokenService
import ltd.guimc.web.altget.service.coin.CoinTransactionHistoryService
import ltd.guimc.web.altget.service.coin.OxaPayRechargeService
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.transaction.annotation.Transactional
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.LocalDate
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
    private val oxaPayRechargeService: OxaPayRechargeService,
    private val userOperationService: UserOperationService,
    private val altConsumptionRecordService: AltConsumptionRecordService,
    private val usedAltCategoryService: UsedAltCategoryService,
    private val currentAdminService: CurrentAdminService,
    private val adminCsvExportService: AdminCsvExportService,
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

    private fun validatePage(page: Int, size: Int) {
        if (page < 1) throw AdminBadRequestException("page must be at least 1")
        if (size !in 1..100) throw AdminBadRequestException("size must be between 1 and 100")
    }

    private fun <T> QueryWrapper<T>.applySort(spec: AdminSortSpec) {
        orderBy(true, spec.ascending, spec.column)
        if (spec.column != spec.tieBreakerColumn) {
            orderBy(true, spec.ascending, spec.tieBreakerColumn)
        }
    }

    private fun tooManyExportRecords(total: Long) {
        if (total > AdminCsvExportService.MAX_EXPORT_RECORDS) {
            throw AdminBadRequestException(
                "Export contains $total records; maximum is ${AdminCsvExportService.MAX_EXPORT_RECORDS}",
            )
        }
    }

    private fun prepareCsvResponse(response: HttpServletResponse, filePrefix: String) {
        response.characterEncoding = Charsets.UTF_8.name()
        response.contentType = "text/csv; charset=UTF-8"
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"$filePrefix-${LocalDate.now()}.csv\"",
        )
    }

    private fun auditExport(userId: Int, type: String, filters: String, count: Long, ip: String) {
        userOperationService.log(
            userId,
            EnumUserOperation.ADMIN_DATA_EXPORT,
            "Exported $type records=$count filters=$filters",
            ip,
        )
    }

    private fun userKeywordQuery(keyword: String?): QueryWrapper<CoreAuth> {
        val normalizedKeyword = keyword?.trim()
        return QueryWrapper<CoreAuth>().apply {
            if (!normalizedKeyword.isNullOrEmpty()) {
                nested { wrapper ->
                    normalizedKeyword.toIntOrNull()?.let { wrapper.eq("user_id", it).or() }
                    wrapper.like("username", normalizedKeyword)
                        .or()
                        .like("email", normalizedKeyword)
                }
            }
        }
    }

    private fun matchingUserIds(keyword: String?): List<Int>? {
        if (keyword?.trim().isNullOrEmpty()) return null
        return coreAuthService.list(userKeywordQuery(keyword)).map { it.userId }
    }

    // </editor-fold>

    // <editor-fold desc="Dashboard Summary">
    @GetMapping("/api/admin/me")
    fun getCurrentAdmin(@CurrentUserId userId: Int?): ResponseBase<AdminProfileResponse> {
        requireAdmin<AdminProfileResponse>(userId)?.let { return it }
        val profile = currentAdminService.getProfile(userId!!)
            ?: return ResponseBase(404, "Admin profile not found")
        return ResponseBase(profile)
    }

    @GetMapping("/api/admin/dashboard")
    fun getDashboardSummary(@CurrentUserId userId: Int?): ResponseBase<AdminDashboardSummaryResponse> {
        requireAdmin<AdminDashboardSummaryResponse>(userId)?.let { return it }
        val recentOperations = userOperationService.getPage(
            1,
            6,
            QueryWrapper<UserOperation>().orderByDesc("operation_time")
        ).records.map { op ->
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
        return ResponseBase(
            AdminDashboardSummaryResponse(
                totalUsers = coreAuthService.count(),
                verifiedUsers = userDetailsService.count(
                    QueryWrapper<UserDetails>().eq("user_role", EnumUserRole.VERIFY.role)
                ),
                availableTokens = coinTokenService.count(
                    QueryWrapper<CoinToken>().eq("is_used", false)
                ),
                paidRechargeOrders = oxaPayRechargeService.count(
                    QueryWrapper<OxaPayRechargeOrder>().eq("status", EnumOxaPayRechargeStatus.PAID.value)
                ),
                recentOperations = recentOperations,
            )
        )
    }
    // </editor-fold>

    // <editor-fold desc="List Users">
    @GetMapping("/api/admin/users")
    fun listUsers(
        @CurrentUserId userId: Int?,
        @RequestParam size: Int = 20,
        @RequestParam page: Int = 1,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
    ): ResponseBase<PageResponse<SimpleUserInfo>> {
        requireAdmin<PageResponse<SimpleUserInfo>>(userId)?.let { return it }
        validatePage(page, size)
        val sort = AdminSort.resolve(AdminList.USERS, sortBy, sortDirection)
        val pageResult = if (sort.column == "user_role") {
            val matchingIds = matchingUserIds(keyword)
            if (matchingIds != null && matchingIds.isEmpty()) {
                return ResponseBase(PageResponse(emptyList(), 0, size.toLong(), page.toLong(), 0))
            }
            val detailsQuery = QueryWrapper<UserDetails>().apply {
                matchingIds?.let { inSqlIds -> `in`("user_id", inSqlIds) }
                applySort(sort)
            }
            userDetailsService.getPage(page, size, detailsQuery)
        } else {
            coreAuthService.getPage(page, size, userKeywordQuery(keyword).apply { applySort(sort) })
        }
        val userInfoList = pageResult.records.mapNotNull { record ->
            val recordUserId = if (record is UserDetails) record.userId else (record as CoreAuth).userId
            recordUserId?.let { buildSimpleUserInfo(it) }
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

    // <editor-fold desc="List OxaPay Recharges">
    @GetMapping("/api/admin/oxapay/recharges")
    fun listOxaPayRecharges(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) filterUserId: Int?,
        @RequestParam(required = false) filterStatus: EnumOxaPayRechargeStatus?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
    ): ResponseBase<PageResponse<AdminOxaPayRechargeResponse>> {
        requireAdmin<PageResponse<AdminOxaPayRechargeResponse>>(userId)?.let { return it }
        validatePage(page, size)
        val sort = AdminSort.resolve(AdminList.OXAPAY_RECHARGES, sortBy, sortDirection)
        val normalizedKeyword = keyword?.trim()
        val queryWrapper = QueryWrapper<OxaPayRechargeOrder>().apply {
            if (!normalizedKeyword.isNullOrEmpty()) {
                nested { wrapper ->
                    wrapper.like("id", normalizedKeyword)
                        .or()
                        .like("track_id", normalizedKeyword)
                }
            }
            filterUserId?.let { eq("user_id", it) }
            filterStatus?.let { eq("status", it.value) }
            applySort(sort)
        }
        val pageResult = oxaPayRechargeService.getPage(page, size, queryWrapper)
        return ResponseBase(PageResponse(
            records = pageResult.records.map(AdminOxaPayRechargeResponse::from),
            total = pageResult.total,
            size = pageResult.size,
            current = pageResult.current,
            pages = pageResult.pages,
        ))
    }
    // </editor-fold>

    // <editor-fold desc="CSV Exports">
    @GetMapping("/api/admin/users/export")
    fun exportUsers(
        @CurrentUserId userId: Int?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RealIP ip: String,
        response: HttpServletResponse,
    ) {
        requireAdmin<Unit>(userId)?.let { return }
        val sort = AdminSort.resolve(AdminList.USERS, sortBy, sortDirection)
        val matchingIds = matchingUserIds(keyword)
        val (total, users) = if (sort.column == "user_role") {
            if (matchingIds != null && matchingIds.isEmpty()) {
                0L to emptyList()
            } else {
                val query = QueryWrapper<UserDetails>().apply {
                    matchingIds?.let { `in`("user_id", it) }
                    applySort(sort)
                }
                userDetailsService.count(query) to userDetailsService.list(query)
                    .mapNotNull { it.userId?.let(::buildSimpleUserInfo) }
            }
        } else {
            val query = userKeywordQuery(keyword).apply { applySort(sort) }
            coreAuthService.count(query) to coreAuthService.list(query)
                .mapNotNull { buildSimpleUserInfo(it.userId) }
        }
        tooManyExportRecords(total)
        prepareCsvResponse(response, "users")
        adminCsvExportService.write(
            response.outputStream,
            listOf("id", "name", "email", "role"),
            users.asSequence().map { listOf(it.id, it.name, it.email, it.role.name) },
        )
        auditExport(userId!!, "users", exportFilters("keyword" to keyword, "sortBy" to sortBy, "sortDirection" to sortDirection), total, ip)
    }

    @GetMapping("/api/admin/tokens/export")
    fun exportTokens(
        @CurrentUserId userId: Int?,
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RealIP ip: String,
        response: HttpServletResponse,
    ) {
        requireAdmin<Unit>(userId)?.let { return }
        val sort = AdminSort.resolve(AdminList.TOKENS, sortBy, sortDirection)
        val query = QueryWrapper<CoinToken>().apply {
            keyword.trim().takeIf { it.isNotEmpty() }?.let { like("id", it) }
            applySort(sort)
        }
        val total = coinTokenService.count(query)
        tooManyExportRecords(total)
        val tokens = coinTokenService.list(query)
        prepareCsvResponse(response, "tokens")
        // Deliberately omit the token id: it is the redeemable token value.
        adminCsvExportService.write(
            response.outputStream,
            listOf("coinAmount", "createdBy", "createdAt", "redeemedBy", "redeemedAt", "isUsed"),
            tokens.asSequence().map {
                listOf(it.coinAmount, it.createdBy, it.createdAt, it.redeemedBy, it.redeemedAt, it.isUsed)
            },
        )
        auditExport(userId!!, "tokens", exportFilters("keyword" to keyword, "sortBy" to sortBy, "sortDirection" to sortDirection), total, ip)
    }

    @GetMapping("/api/admin/operations/export")
    fun exportOperations(
        @CurrentUserId userId: Int?,
        @RequestParam(required = false) filterUserId: Int?,
        @RequestParam(required = false) filterOperation: Int?,
        @RequestParam(required = false) filterTimeStart: String?,
        @RequestParam(required = false) filterTimeEnd: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RealIP ip: String,
        response: HttpServletResponse,
    ) {
        requireAdmin<Unit>(userId)?.let { return }
        val sort = AdminSort.resolve(AdminList.OPERATIONS, sortBy, sortDirection)
        val query = QueryWrapper<UserOperation>()
            .apply { filterUserId?.let { eq("user_id", it) } }
            .apply { filterOperation?.let { eq("operation", it) } }
            .apply { filterTimeStart?.takeIf { it.isNotBlank() }?.let { ge("operation_time", it) } }
            .apply { filterTimeEnd?.takeIf { it.isNotBlank() }?.let { le("operation_time", it) } }
            .apply { applySort(sort) }
        val total = userOperationService.count(query)
        tooManyExportRecords(total)
        val operations = userOperationService.list(query)
        prepareCsvResponse(response, "operations")
        adminCsvExportService.write(
            response.outputStream,
            listOf("operationId", "userId", "username", "operation", "description", "ip", "geoip", "operationTime"),
            operations.asSequence().map {
                listOf(it.operationId, it.userId, it.username, it.operation?.name, it.description, it.ip, it.geoip, it.operationTime)
            },
        )
        auditExport(
            userId!!,
            "operations",
            exportFilters("filterUserId" to filterUserId, "filterOperation" to filterOperation, "filterTimeStart" to filterTimeStart, "filterTimeEnd" to filterTimeEnd, "sortBy" to sortBy, "sortDirection" to sortDirection),
            total,
            ip,
        )
    }

    @GetMapping("/api/admin/used-alts/export")
    fun exportUsedAlts(
        @CurrentUserId userId: Int?,
        @RequestParam(required = false) filterIp: String?,
        @RequestParam(required = false) filterUserId: Int?,
        @RequestParam(required = false) filterChannel: String?,
        @RequestParam(required = false) filterUsername: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RealIP ip: String,
        response: HttpServletResponse,
    ) {
        requireAdmin<Unit>(userId)?.let { return }
        val sort = AdminSort.resolve(AdminList.USED_ALTS, sortBy, sortDirection)
        val normalizedUsername = filterUsername?.trim()
        val query = QueryWrapper<UsedAltCategory>()
            .apply { filterIp?.let { eq("operation_ip", it) } }
            .apply { filterUserId?.let { eq("user_id", it) } }
            .apply { filterChannel?.let { eq("channel", it) } }
            .apply { normalizedUsername?.takeIf { it.isNotEmpty() }?.let { like("username", it) } }
            .apply { applySort(sort) }
        val total = usedAltCategoryService.count(query)
        tooManyExportRecords(total)
        val alts = usedAltCategoryService.list(query)
        prepareCsvResponse(response, "used-alts")
        // Deliberately omit password; this export is an audit report, not a credential dump.
        adminCsvExportService.write(
            response.outputStream,
            listOf("id", "username", "channel", "userId", "operationIp", "fetchMethod", "fetchTime", "createdAt"),
            alts.asSequence().map {
                listOf(it.id, it.username, it.channel, it.userId, it.operationIp, it.fetchMethod, it.fetchTime, it.createdAt)
            },
        )
        auditExport(
            userId!!,
            "used-alts",
            exportFilters("filterIp" to filterIp, "filterUserId" to filterUserId, "filterChannel" to filterChannel, "filterUsername" to filterUsername, "sortBy" to sortBy, "sortDirection" to sortDirection),
            total,
            ip,
        )
    }

    @GetMapping("/api/admin/oxapay/recharges/export")
    fun exportOxaPayRecharges(
        @CurrentUserId userId: Int?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) filterUserId: Int?,
        @RequestParam(required = false) filterStatus: EnumOxaPayRechargeStatus?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RealIP ip: String,
        response: HttpServletResponse,
    ) {
        requireAdmin<Unit>(userId)?.let { return }
        val sort = AdminSort.resolve(AdminList.OXAPAY_RECHARGES, sortBy, sortDirection)
        val normalizedKeyword = keyword?.trim()
        val query = QueryWrapper<OxaPayRechargeOrder>().apply {
            normalizedKeyword?.takeIf { it.isNotEmpty() }?.let {
                nested { wrapper ->
                    wrapper.like("id", it).or().like("track_id", it)
                }
            }
            filterUserId?.let { eq("user_id", it) }
            filterStatus?.let { eq("status", it.value) }
            applySort(sort)
        }
        val total = oxaPayRechargeService.count(query)
        tooManyExportRecords(total)
        val orders = oxaPayRechargeService.list(query)
        prepareCsvResponse(response, "oxapay-recharges")
        adminCsvExportService.write(
            response.outputStream,
            listOf("orderId", "userId", "trackId", "usdAmount", "cnyAmount", "coinAmount", "status", "expiredAt", "paidAt", "createdAt", "updatedAt"),
            orders.asSequence().map {
                listOf(it.id, it.userId, it.trackId, it.usdAmount, it.cnyAmount, it.coinAmount, it.statusAt().name, it.expiredAt, it.paidAt, it.createdAt, it.updatedAt)
            },
        )
        auditExport(
            userId!!,
            "oxapay-recharges",
            exportFilters("keyword" to keyword, "filterUserId" to filterUserId, "filterStatus" to filterStatus, "sortBy" to sortBy, "sortDirection" to sortDirection),
            total,
            ip,
        )
    }

    private fun exportFilters(vararg values: Pair<String, Any?>): String = values
        .filter { it.second != null && it.second.toString().isNotBlank() }
        .joinToString(",") { (name, value) -> "$name=$value" }
    // </editor-fold>

    // <editor-fold desc="Get OxaPay Recharge">
    @GetMapping("/api/admin/oxapay/recharge/{orderId}")
    fun getOxaPayRecharge(
        @CurrentUserId userId: Int?,
        @PathVariable orderId: String,
    ): ResponseBase<AdminOxaPayRechargeResponse> {
        requireAdmin<AdminOxaPayRechargeResponse>(userId)?.let { return it }
        val order = oxaPayRechargeService.getById(orderId)
            ?: return ResponseBase(404, "Recharge order not found")
        return ResponseBase(AdminOxaPayRechargeResponse.from(order))
    }
    // </editor-fold>

    // <editor-fold desc="Manage OxaPay Recharge">
    @PostMapping("/api/admin/oxapay/recharge/{orderId}/status")
    fun updateOxaPayRechargeStatus(
        @CurrentUserId userId: Int?,
        @PathVariable orderId: String,
        @RequestParam status: EnumOxaPayRechargeStatus,
        @RealIP ip: String,
    ): ResponseBase<AdminOxaPayRechargeResponse> {
        requireAdmin<AdminOxaPayRechargeResponse>(userId)?.let { return it }
        return try {
            val order = oxaPayRechargeService.updateStatus(orderId, status)
                ?: return ResponseBase(404, "Recharge order not found")
            userOperationService.log(
                userId!!,
                EnumUserOperation.ADMIN_OXAPAY_STATUS,
                "Updated OxaPay recharge $orderId status to ${order.status}",
                ip,
            )
            ResponseBase(AdminOxaPayRechargeResponse.from(order))
        } catch (e: IllegalArgumentException) {
            ResponseBase(400, e.message ?: "Invalid OxaPay recharge status")
        } catch (e: IllegalStateException) {
            ResponseBase(400, e.message ?: "OxaPay recharge status cannot be changed")
        }
    }

    @PostMapping("/api/admin/oxapay/recharge/{orderId}/top-up")
    fun topUpOxaPayRecharge(
        @CurrentUserId userId: Int?,
        @PathVariable orderId: String,
        @RealIP ip: String,
    ): ResponseBase<AdminOxaPayRechargeResponse> {
        requireAdmin<AdminOxaPayRechargeResponse>(userId)?.let { return it }
        return try {
            val result = oxaPayRechargeService.topUp(orderId)
                ?: return ResponseBase(404, "Recharge order not found")
            val order = result.order
            if (result.credited) {
                userOperationService.log(
                    userId!!,
                    EnumUserOperation.ADMIN_OXAPAY_TOP_UP,
                    "Topped up OxaPay recharge $orderId with ${order.coinAmount} credits",
                    ip,
                )
            }
            ResponseBase(AdminOxaPayRechargeResponse.from(order))
        } catch (e: IllegalArgumentException) {
            ResponseBase(400, e.message ?: "Invalid OxaPay recharge top-up")
        } catch (e: IllegalStateException) {
            ResponseBase(400, e.message ?: "OxaPay recharge cannot be topped up")
        }
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
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
    ): ResponseBase<PageResponse<CoinTokenResponse>> {
        requireAdmin<PageResponse<CoinTokenResponse>>(userId)?.let { return it }
        validatePage(page, size)
        val sort = AdminSort.resolve(AdminList.TOKENS, sortBy, sortDirection)
        val queryWrapper = QueryWrapper<CoinToken>().apply {
            if (keyword.trim().isNotEmpty()) {
                nested { wrapper ->
                    wrapper.like("id", keyword.trim())
                }
            }
            applySort(sort)
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
        @RequestParam(required = false) filterTimeEnd: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
    ): ResponseBase<PageResponse<AdminOperationLogResponse>> {
        requireAdmin<PageResponse<AdminOperationLogResponse>>(userId)?.let { return it }
        validatePage(page, size)
        val sort = AdminSort.resolve(AdminList.OPERATIONS, sortBy, sortDirection)
        val queryWrapper = QueryWrapper<UserOperation>()
            .apply { filterUserId?.let { eq("user_id", it) } }
            .apply { filterOperation?.let { eq("operation", it) } }
            .apply { filterTimeStart?.let { ge("operation_time", it) } }
            .apply { filterTimeEnd?.let { le("operation_time", it) } }
            .apply { applySort(sort) }
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

    // <editor-fold desc="Get Operation Log">
    @GetMapping("/api/admin/operation/{operationId}")
    fun getOperation(
        @CurrentUserId userId: Int?,
        @PathVariable operationId: String,
    ): ResponseBase<AdminOperationLogResponse> {
        requireAdmin<AdminOperationLogResponse>(userId)?.let { return it }
        val operation = userOperationService.getById(operationId)
            ?: return ResponseBase(404, "Operation not found")
        return ResponseBase(
            AdminOperationLogResponse(
                operationId = operation.operationId ?: "",
                operationTime = operation.operationTime.toString(),
                userId = operation.userId,
                username = operation.username ?: "",
                operation = operation.operation ?: EnumUserOperation.LOGIN,
                description = operation.description ?: "",
                ip = operation.ip,
                geoip = operation.geoip,
            )
        )
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
        @RequestParam(required = false) filterUsername: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(defaultValue = "desc") sortDirection: String,
    ): ResponseBase<PageResponse<UsedAltResponse>> {
        requireAdmin<PageResponse<UsedAltResponse>>(userId)?.let { return it }
        validatePage(page, size)
        val sort = AdminSort.resolve(AdminList.USED_ALTS, sortBy, sortDirection)
        val normalizedUsername = filterUsername?.trim()
        val queryWrapper = QueryWrapper<UsedAltCategory>()
            .apply { filterIp?.let { eq("operation_ip", it) } }
            .apply { filterUserId?.let { eq("user_id", it) } }
            .apply { filterChannel?.let { eq("channel", it) } }
            .apply { if (!normalizedUsername.isNullOrEmpty()) like("username", normalizedUsername) }
            .apply { applySort(sort) }
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

    // <editor-fold desc="Get Used Alt">
    @GetMapping("/api/admin/used-alt/{id}")
    fun getUsedAlt(
        @CurrentUserId userId: Int?,
        @PathVariable id: Long,
    ): ResponseBase<UsedAltResponse> {
        requireAdmin<UsedAltResponse>(userId)?.let { return it }
        val alt = usedAltCategoryService.getById(id)
            ?: return ResponseBase(404, "Used alt not found")
        return ResponseBase(
            UsedAltResponse(
                id = alt.id,
                username = alt.username,
                password = alt.password,
                channel = alt.channel,
                userId = alt.userId,
                operationIp = alt.operationIp,
                fetchMethod = alt.fetchMethod,
                fetchTime = alt.fetchTime.toString(),
                createdAt = alt.createdAt?.toString(),
            )
        )
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
