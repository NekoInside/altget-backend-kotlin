package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.entity.db.user.UserApi
import ltd.guimc.web.altget.entity.request.user.ApiKeyRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.user.UserApiKeyInfo
import ltd.guimc.web.altget.entity.response.user.UserInfo
import ltd.guimc.web.altget.entity.response.user.UserOperationLogResponse
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.service.pow.PoWTaskService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/api/user")
class UserController(
    private val coreAuthService: CoreAuthService,
    private val userDetailsService: UserDetailsService,
    private val userApiService: UserApiService,
    private val userOperationService: UserOperationService,
    private val geetestVerifyComponent: GeetestVerifyComponent,
    private val poWTaskService: PoWTaskService
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
    @GetMapping("/self")
    fun getSelf(@CurrentUserId userId: Int?): ResponseBase<UserInfo> {
        requireAuth<UserInfo>(userId)?.let { return it }
        val userInfo = buildUserInfo(userId!!)
            ?: return ResponseBase(500, "Failed to retrieve user information")
        return ResponseBase(userInfo)
    }
    // </editor-fold>

    // <editor-fold desc="Get Self Operation Log">
    @GetMapping("/self/operations")
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
    @GetMapping("/self/api-key")
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
    @PostMapping("/self/api-key/new")
    @Transactional
    fun newApiKey(@CurrentUserId userId: Int?, @RequestBody request: ApiKeyRequest): ResponseBase<UserApiKeyInfo> {
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
        userOperationService.log(userId, EnumUserOperation.API_KEY_NEW, "Created new API key")
        val updatedApi = userApiService.getById(userId)
            ?: return ResponseBase(500, "Failed to create API key")
        return ResponseBase(UserApiKeyInfo(
            apiKey = updatedApi.apiKey,
            limitLevel = updatedApi.limitLevel
        ))
    }
    // </editor-fold>

    // <editor-fold desc="Rotate User API Key">
    @PostMapping("/self/api-key/rotate")
    @Transactional
    fun rotateApiKey(@CurrentUserId userId: Int?, @RequestBody request: ApiKeyRequest): ResponseBase<UserApiKeyInfo> {
        requireAuth<UserApiKeyInfo>(userId)?.let { return it }
        verifyPoWAndCaptcha<UserApiKeyInfo>(request, "new-api")?.let { return it }

        val existingApi = userApiService.getById(userId!!)
            ?: return ResponseBase(404, "API key not found. Please create one first.")
        val newApiKey = UUID.randomUUID().toString()
        userApiService.updateById(existingApi.apply {
            apiKey = newApiKey
        })
        userOperationService.log(userId, EnumUserOperation.API_KEY_ROTATE, "Rotated API key")
        val updatedApi = userApiService.getById(userId)
            ?: return ResponseBase(500, "Failed to rotate API key")
        return ResponseBase(UserApiKeyInfo(
            apiKey = updatedApi.apiKey,
            limitLevel = updatedApi.limitLevel
        ))
    }
    // </editor-fold>
}
