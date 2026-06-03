package ltd.guimc.web.altget.controller

import cn.hutool.core.net.url.UrlBuilder
import cn.hutool.json.JSONObject
import com.nimbusds.srp6.SRP6CryptoParams
import com.nimbusds.srp6.SRP6ServerSession
import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.AssertionResult
import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RegistrationResult
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import com.yubico.webauthn.exception.AssertionFailedException
import com.yubico.webauthn.exception.RegistrationFailedException
import jakarta.servlet.http.HttpServletResponse
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.component.EmailComponent
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.component.JwtTokenComponent
import ltd.guimc.web.altget.config.SiteProperities
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.entity.db.passkey.PasskeyCredentialEntity
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.entity.db.user.UserOauth
import ltd.guimc.web.altget.entity.request.auth.ForgotPasswordRequest
import ltd.guimc.web.altget.entity.request.auth.LoginVerifyRequest
import ltd.guimc.web.altget.entity.request.auth.RegisterRequest
import ltd.guimc.web.altget.entity.request.auth.ResetPasswordRequest
import ltd.guimc.web.altget.entity.request.passkey.PasskeyLoginOptionsRequest
import ltd.guimc.web.altget.entity.request.passkey.PasskeyLoginVerifyRequest
import ltd.guimc.web.altget.entity.request.passkey.PasskeyRegisterVerifyRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.auth.LoginChallengeResponse
import ltd.guimc.web.altget.entity.response.auth.LoginVerifyResponse
import ltd.guimc.web.altget.entity.response.passkey.PasskeyCredentialResponse
import ltd.guimc.web.altget.entity.response.passkey.PasskeyOptionsResponse
import ltd.guimc.web.altget.enum.EnumOauthUsage
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.passkey.PasskeyCredentialService
import ltd.guimc.web.altget.service.passkey.WebAuthnChallengeCacheService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.geolocation.GeolocationService
import ltd.guimc.web.altget.service.user.UserOauthService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@RestController
class AuthController(
    private val geetestVerifyComponent: GeetestVerifyComponent,
    private val emailComponent: EmailComponent,
    private val jwtTokenComponent: JwtTokenComponent,
    private val siteProperities: SiteProperities,
    private val coreAuthService: CoreAuthService,
    private val userOauthService: UserOauthService,
    private val userCoinService: UserCoinService,
    @Qualifier("objectRedisTemplate")
    private val objectRedisTemplate: RedisTemplate<String, Any>,
    private val userDetailsService: UserDetailsService,
    private val userOperationService: UserOperationService,
    private val geolocationService: GeolocationService,
    private val relyingParty: RelyingParty,
    private val passkeyCredentialService: PasskeyCredentialService,
    private val webAuthnChallengeCacheService: WebAuthnChallengeCacheService
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)
    // <editor-fold desc="用户注册">
    @PostMapping("/api/auth/register")
    fun register(@RequestBody request: RegisterRequest): ResponseBase<String> {
        if (!geetestVerifyComponent.verify(request)) {
            return ResponseBase(400, "人机验证校验失败")
        }
        if (!isValidEmail(request.email)) {
            return ResponseBase(400, "邮箱格式不正确")
        }
        if (!isValidUsername(request.username)) {
            return ResponseBase(400, "用户名格式不正确")
        }
        if (coreAuthService.getByUsername(request.username) != null ||
            coreAuthService.getByEmail(request.email) != null) {
            Thread.sleep(3000)
            return ResponseBase("若该用户名或邮箱未被注册，我们将发送一封验证邮件到该邮箱，请注意查收")
        }
        // 初始化数据库值
        coreAuthService.save(CoreAuth().apply {
            email = request.email
            username = request.username
            srpSalt = request.salt
            srpVerifier = request.verifier
        })
        // 重新获取 CoreAuth 示例以获取 ID
        val savedCoreAuth =
            coreAuthService.getByUsername(request.username) ?: return ResponseBase(500, "用户注册失败，请稍后再试")
        // 理论上来说不应该失败 除非数据库掉了
        userOauthService.save(UserOauth().apply {
            userId = savedCoreAuth.userId
        })
        userCoinService.save(UserCoin().apply {
            userId = savedCoreAuth.userId
            balance = 0
        })
        userDetailsService.save(UserDetails().apply {
            userId = savedCoreAuth.userId
        })
        // 前面的步骤都成功了 说明用户注册成功了 现在需要发送验证邮件
        val token = jwtTokenComponent.generateRegisterVerifyToken(request.email)
        val fullActiveUrl = "https://" + siteProperities.domain + "/activate?token=" + URLEncoder.encode(token, "UTF-8")
        emailComponent.sendActivationEmail(request.email, request.username, fullActiveUrl)
        return ResponseBase("若该用户名或邮箱未被注册，我们将发送一封验证邮件到该邮箱，请注意查收")
    }

    @GetMapping("/api/auth/activate")
    fun activate(code: String): ResponseBase<String> {
        val email = jwtTokenComponent.getEmailFromToken(code) ?: return ResponseBase(400, "无效的激活链接")
        val coreAuthEntity = coreAuthService.getByEmail(email) ?: return ResponseBase(400, "无效的激活链接")
        val userDetails: UserDetails = userDetailsService.getById(coreAuthEntity.userId) ?: return ResponseBase(400, "无效的激活链接")
        if (userDetails.userRole != EnumUserRole.UNVERIFY) return ResponseBase(400, "无效的激活链接")
        userDetailsService.updateById(userDetails.apply {
            userRole = EnumUserRole.VERIFY
        })
        return ResponseBase("账户激活成功，您现在可以使用账号密码登录了")
    }
    // </editor-fold>

    // <editor-fold desc="忘记密码">
    @PostMapping("/api/auth/forgot-password")
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseBase<String> {
        if (!geetestVerifyComponent.verify(request)) {
            return ResponseBase(400, "人机验证校验失败")
        }
        val coreAuth = coreAuthService.getByEmail(request.email)
        if (coreAuth != null) {
            val token = jwtTokenComponent.generatePasswordResetToken(request.email)
            // 将重置唯一标识存入 Redis，实现有状态防重放
            val resetId = jwtTokenComponent.getPasswordResetIdFromToken(token)
            if (resetId != null) {
                objectRedisTemplate.opsForValue().set(
                    "password-reset:$resetId",
                    coreAuth.userId.toString(),
                    jwtTokenComponent.jwtProperties.registerVerifyTokenExpiration
                )
            }
            val resetUrl = "https://" + siteProperities.domain + "/reset-password?token=" + URLEncoder.encode(token, "UTF-8")
            emailComponent.sendPasswordResetEmail(request.email, coreAuth.username ?: "", resetUrl)
        }
        // Always return the same message to prevent email enumeration
        Thread.sleep(3000)
        return ResponseBase("若该邮箱已注册，我们将发送一封密码重置邮件到该邮箱，请注意查收")
    }

    @PostMapping("/api/auth/reset-password")
    @Transactional
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseBase<String> {
        // 提取并校验防重放标识：每个 token 只能使用一次
        val resetId = jwtTokenComponent.getPasswordResetIdFromToken(request.token)
            ?: return ResponseBase(400, "无效的重置链接")
        val redisKey = "password-reset:$resetId"
        val consumed = objectRedisTemplate.opsForValue().getAndDelete(redisKey)
            ?: return ResponseBase(400, "重置链接已失效或已被使用")
        // 验证 JWT 是否过期
        if (!jwtTokenComponent.isJWTVaild(request.token)) {
            return ResponseBase(400, "重置链接已过期")
        }
        val email = jwtTokenComponent.getEmailFromToken(request.token)
            ?: return ResponseBase(400, "无效的重置链接")
        val coreAuth = coreAuthService.getByEmail(email)
            ?: return ResponseBase(400, "无效的重置链接")
        coreAuthService.updateById(coreAuth.apply {
            srpSalt = request.salt
            srpVerifier = request.verifier
        })
        userOperationService.log(coreAuth.userId, EnumUserOperation.PASSWORD_RESET, "通过邮箱重置密码")
        return ResponseBase("密码重置成功，您现在可以使用新密码登录了")
    }
    // </editor-fold>

    // <editor-fold desc="用户账号密码登录">
    @GetMapping("/api/auth/password/challenge")
    fun passwordChallenge(username: String): ResponseBase<LoginChallengeResponse> {
        val coreAuthEntity = coreAuthService.getByUsername(username)
        var saltHex: String
        var verifierHex: String
        var saveSessionId = false
        if (coreAuthEntity != null) {
            saltHex = coreAuthEntity.srpSalt ?: return ResponseBase(500, "用户数据异常，请联系管理员")
            verifierHex = coreAuthEntity.srpVerifier ?: return ResponseBase(500, "用户数据异常，请联系管理员")
            saveSessionId = true
        } else {
            // 用户不存在时返回伪造的 salt 和 verifier，防止用户枚举攻击
            saltHex = generateFakeSalt(username)
            // 生成一个 2048 位的假 Verifier 防止我们自己的流程报错
            verifierHex = "00" + "00".repeat(256 - 1)
        }

        val config = SRP6CryptoParams.getInstance(2048, "SHA-256")
        val serverSession = SRP6ServerSession(config)

        val salt = BigInteger(saltHex, 16)
        val verifier = BigInteger(verifierHex, 16)

        val b = serverSession.step1(username, salt, verifier)
        val sessionId = UUID.randomUUID().toString()

        if (saveSessionId) {
            objectRedisTemplate.opsForValue().set("srp:session:$sessionId", serverSession, Duration.ofSeconds(
                siteProperities.srpSessionTtl.toLong()
            ))
        }

        return ResponseBase( LoginChallengeResponse(sessionId, saltHex, b.toString(16)))
    }

    @PostMapping("/api/auth/password/token")
    fun passwordVerify(@RequestBody request: LoginVerifyRequest, @RealIP ip: String?): ResponseBase<LoginVerifyResponse> {
        var serverSession: SRP6ServerSession?
        try {
            serverSession = objectRedisTemplate.opsForValue().get("srp:session:${request.sessionId}") as? SRP6ServerSession
        } catch (_: Exception) {
            // 与密码错误一样的响应
            return ResponseBase(400, "用户名或密码错误")
        }
        if (serverSession == null) {
            return ResponseBase(400, "用户名或密码错误")
        }

        val a = BigInteger(request.a, 16)
        val m1 = BigInteger(request.m1, 16)

        try {
            val m2 = serverSession.step2(a, m1)
            val username = serverSession.getUserID()
            val coreAuthEntity = coreAuthService.getByUsername(username) ?: return ResponseBase(400, "服务器 SRP Session 异常")
            val userDetails = userDetailsService.getById(coreAuthEntity.userId) ?: return ResponseBase(400, "服务器 SRP Session 异常")
            if (userDetails.userRole == EnumUserRole.UNVERIFY) return ResponseBase(400, "账号未激活，请先激活账号")
            if (userDetails.userRole == EnumUserRole.BANNED) return ResponseBase(400, "账号已被封禁，请联系管理员")
            // 更新最后登录信息
            userDetails.lastLoginTime = LocalDateTime.now()
            userDetails.lastLoginIp = ip ?: ""
            userDetails.lastLoginGeo = if (!ip.isNullOrBlank()) geolocationService.formatLocationMessage(geolocationService.getGeolocation(ip)) else ""
            userDetailsService.updateById(userDetails)
            val jwtToken = jwtTokenComponent.generateLoginSession(coreAuthEntity.userId)
            userOperationService.log(coreAuthEntity.userId, EnumUserOperation.LOGIN, "账号密码登录")
            return ResponseBase(LoginVerifyResponse(m2.toString(16), jwtToken))
        } catch (_: Exception) {
            return ResponseBase(400, "用户名或密码错误")
        }
    }


    private fun generateFakeSalt(username: String): String {
        try {
            val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
            val input = username + siteProperities.fakeAccountSalt
            val hash: ByteArray = digest.digest(input.toByteArray(charset("UTF-8")))

            val hexString = StringBuilder()
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        } catch (e: Exception) {
            throw RuntimeException("生成假Salt失败", e)
        }
    }
    // </editor-fold>

    // <editor-fold desc="GitHub OAuth">
    @GetMapping("/api/auth/github")
    fun githubOAuth(response: HttpServletResponse, usage: String?): ResponseBase<String> {
        val clientId = siteProperities.githubClientId
        val redirectUri = "https://${siteProperities.domain}/github-callback"
        val state = UUID.randomUUID().toString()
        val oauthUsage = if (usage == "bind") EnumOauthUsage.BIND else EnumOauthUsage.LOGIN
        // 将 state 存储在 Redis 中，设置过期时间为 10 分钟
        objectRedisTemplate.opsForValue().set("oauth:github:state:$state", oauthUsage.name, Duration.ofMinutes(10))
        val oauthUrl = UrlBuilder.of("https://github.com/login/oauth/authorize")
            .addQuery("client_id", clientId)
            .addQuery("redirect_uri", redirectUri)
            .addQuery("state", state)
            .addQuery("scope", "user")
            .build()
        response.sendRedirect(oauthUrl)
        return ResponseBase(200, "OK")
    }

    @GetMapping("/api/auth/github/state")
    fun getStateUsage(state: String): ResponseBase<String> {
        val stored = objectRedisTemplate.opsForValue().get("oauth:github:state:$state") as? String
            ?: return ResponseBase(400, "无效的 state")
        return ResponseBase(if (stored == EnumOauthUsage.LOGIN.name) "login" else "bind")
    }

    @GetMapping("/api/auth/github/login")
    fun processLoginState(code: String, state: String, @RealIP ip: String?): ResponseBase<String> {
        val storedState = objectRedisTemplate.opsForValue().get("oauth:github:state:$state") as? String ?: return ResponseBase(400, "无效的 state")
        objectRedisTemplate.delete("oauth:github:state:$state")
        if (storedState != EnumOauthUsage.LOGIN.name) {
            return ResponseBase(400, "无效的 state")
        }
        val accessToken = try {
            getGithubAccessToken(code)
        } catch (_: Exception) {
            return ResponseBase(500, "获取 GitHub 访问令牌失败")
        }
        val githubUserId = try {
            getGithubUserId(accessToken)
        } catch (_: Exception) {
            return ResponseBase(500, "获取 GitHub 用户信息失败")
        }
        val userId = userOauthService.getUserIdByGithubId(githubUserId) ?: return ResponseBase(400, "该 GitHub 账号未绑定账号")
        val userDetails = userDetailsService.getById(userId) ?: return ResponseBase(400, "服务器 SRP Session 异常")
        if (userDetails.userRole == EnumUserRole.UNVERIFY) return ResponseBase(400, "账号未激活，请先激活账号")
        if (userDetails.userRole == EnumUserRole.BANNED) return ResponseBase(400, "账号已被封禁，请联系管理员")
        // 更新最后登录信息
        userDetails.lastLoginTime = LocalDateTime.now()
        userDetails.lastLoginIp = ip ?: ""
        userDetails.lastLoginGeo = if (!ip.isNullOrBlank()) geolocationService.formatLocationMessage(geolocationService.getGeolocation(ip)) else ""
        userDetailsService.updateById(userDetails)
        val jwtToken = jwtTokenComponent.generateLoginSession(userId)
        userOperationService.log(userId, EnumUserOperation.LOGIN, "Github OAuth 登录")
        return ResponseBase(jwtToken)
    }

    @GetMapping("/api/auth/github/bind")
    fun processBindState(code: String, state: String, @CurrentUserId userId: Int?): ResponseBase<String> {
        val storedState = objectRedisTemplate.opsForValue().get("oauth:github:state:$state") as? String ?: return ResponseBase(400, "无效的 state")
        objectRedisTemplate.delete("oauth:github:state:$state")
        if (storedState != EnumOauthUsage.BIND.name) {
            return ResponseBase(400, "无效的 state")
        }
        if (userId == null) {
            return ResponseBase(401, "请先登录")
        }
        val accessToken = try {
            getGithubAccessToken(code)
        } catch (_: Exception) {
            return ResponseBase(500, "获取 GitHub 访问令牌失败")
        }
        val githubUserId = try {
            getGithubUserId(accessToken)
        } catch (_: Exception) {
            return ResponseBase(500, "获取 GitHub 用户信息失败")
        }
        if (userOauthService.getUserIdByGithubId(githubUserId) != null) {
            return ResponseBase(400, "该 GitHub 账号已绑定账号")
        }
        userOauthService.setGithubId(userId, githubUserId)
        userOperationService.log(userId, EnumUserOperation.OAUTH_BIND, "Github OAuth 绑定")
        return ResponseBase(200, "OK")
    }

    private fun getGithubAccessToken(code: String?): String? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://github.com/login/oauth/access_token"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"client_id\":\"" + siteProperities.githubClientId + "\",\"client_secret\":\"" + siteProperities.githubClientSecret + "\",\"code\":\"" + code + "\"}"))
            .build()
        val client = HttpClient.newHttpClient()
        val res = client.send(request, HttpResponse.BodyHandlers.ofString())
        val json = JSONObject(res.body())
        return json.getStr("access_token")
    }

    private fun getGithubUserId(accessToken: String?): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/user"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .build()
        val client = HttpClient.newHttpClient()
        val res = client.send(request, HttpResponse.BodyHandlers.ofString())
        val json = JSONObject(res.body())
        return json.getLong("id").toString()
    }
    // </editor-fold>

    // <editor-fold desc="Passkey 登录">
    private fun <T> requirePasskeyAuth(currentUserId: Int?): ResponseBase<T>? {
        if (currentUserId == null) {
            return ResponseBase(401, "Not logged in")
        }
        return null
    }

    @PostMapping("/api/auth/passkey/register/options")
    fun passkeyRegisterOptions(
        @CurrentUserId userId: Int?
    ): ResponseBase<PasskeyOptionsResponse> {
        requirePasskeyAuth<PasskeyOptionsResponse>(userId)?.let { return it }
        val uid = userId!!

        val user = coreAuthService.getById(uid)
            ?: return ResponseBase(400, "User not found")

        val userHandleB64 = passkeyCredentialService.getOrCreateUserHandle(uid)

        val userIdentity: UserIdentity
        try {
            userIdentity = UserIdentity.builder()
                .name(user.username ?: "user_$uid")
                .displayName(user.username ?: "user_$uid")
                .id(com.yubico.webauthn.data.ByteArray.fromBase64Url(userHandleB64))
                .build()
        } catch (e: Exception) {
            log.error("Invalid user handle for user {}", uid, e)
            return ResponseBase(500, "Internal error")
        }

        val options = StartRegistrationOptions.builder()
            .user(userIdentity)
            .authenticatorSelection(
                AuthenticatorSelectionCriteria.builder()
                    .residentKey(ResidentKeyRequirement.PREFERRED)
                    .userVerification(UserVerificationRequirement.PREFERRED)
                    .build()
            )
            .build()

        val creationOptions: PublicKeyCredentialCreationOptions = relyingParty.startRegistration(options)

        try {
            val challengeId = UUID.randomUUID().toString()
            val serialized = creationOptions.toJson()
            webAuthnChallengeCacheService.storeChallenge("reg:$challengeId", serialized)
            // Store userHandle explicitly so it can't be lost during JSON round-trip
            webAuthnChallengeCacheService.storeChallenge("reg:uh:$challengeId", userHandleB64)

            return ResponseBase(
                PasskeyOptionsResponse(
                    challengeId = challengeId,
                    options = creationOptions.toCredentialsCreateJson()
                )
            )
        } catch (e: Exception) {
            log.error("Failed to serialize registration options", e)
            return ResponseBase(500, "Internal error")
        }
    }

    @PostMapping("/api/auth/passkey/register/verify")
    fun passkeyRegisterVerify(
        @CurrentUserId userId: Int?,
        @RequestBody request: PasskeyRegisterVerifyRequest
    ): ResponseBase<String> {
        requirePasskeyAuth<String>(userId)?.let { return it }
        val uid = userId!!

        val storedOptionsJson = webAuthnChallengeCacheService.consumeChallenge("reg:${request.challengeId}")
            ?: return ResponseBase(400, "Challenge expired or invalid")

        return try {
            val creationOptions = PublicKeyCredentialCreationOptions.fromJson(storedOptionsJson)
            val pkc = PublicKeyCredential.parseRegistrationResponseJson(request.credential)

            val finishOptions = FinishRegistrationOptions.builder()
                .request(creationOptions)
                .response(pkc)
                .build()

            val result: RegistrationResult = relyingParty.finishRegistration(finishOptions)

            // Retrieve the exact userHandle that was sent to the authenticator during
            // passkeyRegisterOptions. We store it explicitly in Redis to avoid any
            // potential JSON round-trip issues with creationOptions.user.id.
            val userHandleB64 = webAuthnChallengeCacheService.consumeChallenge("reg:uh:${request.challengeId}")
                ?: creationOptions.user.id.base64Url

            val entity = PasskeyCredentialEntity().apply {
                this.userId = uid
                credentialId = result.keyId.id.base64Url
                publicKeyCose = result.publicKeyCose.base64Url
                signatureCount = result.signatureCount
                userHandle = userHandleB64
                credentialName = request.name ?: "My Passkey"
                discoverable = result.isDiscoverable.orElse(false) ?: false
                createdAt = LocalDateTime.now()
            }
            passkeyCredentialService.save(entity)

            userOperationService.log(uid, EnumUserOperation.PASSKEY_REGISTER, "Passkey注册")
            ResponseBase("Passkey registered successfully")
        } catch (e: RegistrationFailedException) {
            log.error("Passkey registration failed", e)
            ResponseBase(400, "Passkey registration failed")
        } catch (e: Exception) {
            log.error("Failed to parse passkey registration response", e)
            ResponseBase(400, "Invalid credential data")
        }
    }

    @PostMapping("/api/auth/passkey/login/options")
    fun passkeyLoginOptions(
        @RequestBody(required = false) request: PasskeyLoginOptionsRequest?
    ): ResponseBase<PasskeyOptionsResponse> {
        val username = request?.username

        val builder = StartAssertionOptions.builder()
            .userVerification(UserVerificationRequirement.PREFERRED)

        if (!username.isNullOrBlank()) {
            builder.username(username)
        }

        val assertionRequest: AssertionRequest = relyingParty.startAssertion(builder.build())

        return try {
            val challengeId = UUID.randomUUID().toString()
            val serialized = assertionRequest.toJson()
            webAuthnChallengeCacheService.storeChallenge("auth:$challengeId", serialized)

            ResponseBase(
                PasskeyOptionsResponse(
                    challengeId = challengeId,
                    options = assertionRequest.toCredentialsGetJson()
                )
            )
        } catch (e: Exception) {
            log.error("Failed to serialize assertion options", e)
            ResponseBase(500, "Internal error")
        }
    }

    @PostMapping("/api/auth/passkey/login/verify")
    fun passkeyLoginVerify(
        @RequestBody request: PasskeyLoginVerifyRequest,
        @RealIP ip: String?
    ): ResponseBase<String> {
        val storedRequestJson = webAuthnChallengeCacheService.consumeChallenge("auth:${request.challengeId}")
            ?: return ResponseBase(400, "Challenge expired or invalid")

        return try {
            val assertionRequest = AssertionRequest.fromJson(storedRequestJson)
            val pkc = PublicKeyCredential.parseAssertionResponseJson(request.credential)

            val finishOptions = FinishAssertionOptions.builder()
                .request(assertionRequest)
                .response(pkc)
                .build()

            val result: AssertionResult = relyingParty.finishAssertion(finishOptions)

            if (!result.isSuccess) {
                return ResponseBase(400, "Passkey authentication failed")
            }

            // Update signature counter
            val cred = passkeyCredentialService.getByCredentialId(
                result.credential.credentialId.base64Url
            )
            if (cred != null) {
                passkeyCredentialService.updateSignatureCount(cred.id, result.signatureCount)
            }

            // Look up the user
            val username = result.username
            val user = coreAuthService.getByUsername(username)
                ?: return ResponseBase(400, "User not found")

            // Standard checks (same as existing login flow)
            val userDetails = userDetailsService.getById(user.userId)
                ?: return ResponseBase(500, "User details not found")

            if (userDetails.userRole == EnumUserRole.UNVERIFY) {
                return ResponseBase(400, "User not verified")
            }
            if (userDetails.userRole == EnumUserRole.BANNED) {
                return ResponseBase(400, "User banned")
            }

            // Update last login info
            userDetails.lastLoginTime = LocalDateTime.now()
            userDetails.lastLoginIp = ip ?: ""
            userDetails.lastLoginGeo = if (!ip.isNullOrBlank()) geolocationService.formatLocationMessage(geolocationService.getGeolocation(ip)) else ""
            userDetailsService.updateById(userDetails)

            // Issue token
            val webToken = jwtTokenComponent.generateLoginSession(user.userId)
            userOperationService.log(user.userId, EnumUserOperation.PASSKEY_LOGIN, "Passkey登录")
            ResponseBase(webToken)
        } catch (e: AssertionFailedException) {
            log.error("Passkey assertion failed: {}", e.message, e)
            ResponseBase(400, "Passkey authentication failed: ${e.message}")
        } catch (e: Exception) {
            log.error("Failed to parse passkey assertion response: {}", e.message, e)
            ResponseBase(400, "Invalid credential data: ${e.message}")
        }
    }

    @GetMapping("/api/auth/passkey/list")
    fun passkeyList(
        @CurrentUserId userId: Int?
    ): ResponseBase<List<PasskeyCredentialResponse>> {
        requirePasskeyAuth<List<PasskeyCredentialResponse>>(userId)?.let { return it }

        val creds = passkeyCredentialService.getCredentialsByUserId(userId!!)
        val result = creds.map { c ->
            PasskeyCredentialResponse(
                id = c.id,
                name = c.credentialName,
                createdAt = c.createdAt.toString()
            )
        }
        return ResponseBase(result)
    }

    @DeleteMapping("/api/auth/passkey/{id}")
    fun passkeyDelete(
        @PathVariable id: Int,
        @CurrentUserId userId: Int?
    ): ResponseBase<String> {
        requirePasskeyAuth<String>(userId)?.let { return it }

        return if (passkeyCredentialService.deleteCredential(id, userId!!)) {
            ResponseBase("Deleted")
        } else {
            ResponseBase(400, "Passkey not found")
        }
    }

    // </editor-fold>

    // <editor-fold desc="校验器">
    private fun isValidEmail(email: String?): Boolean {
        if (email == null) {
            return false
        }
        return email.matches("^\\d+@qq\\.com$".toRegex())
    }

    private fun isValidUsername(username: String): Boolean {
        return username.matches("(?=.{3,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])".toRegex())
    }
    // </editor-fold>
}
