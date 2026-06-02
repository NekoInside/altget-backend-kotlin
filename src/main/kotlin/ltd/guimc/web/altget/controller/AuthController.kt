package ltd.guimc.web.altget.controller

import cn.hutool.core.net.url.UrlBuilder
import cn.hutool.json.JSONObject
import com.nimbusds.srp6.SRP6CryptoParams
import com.nimbusds.srp6.SRP6ServerSession
import jakarta.servlet.http.HttpServletResponse
import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.component.EmailComponent
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.component.JwtTokenComponent
import ltd.guimc.web.altget.config.SiteProperities
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.entity.db.user.UserOauth
import ltd.guimc.web.altget.entity.request.auth.LoginVerifyRequest
import ltd.guimc.web.altget.entity.request.auth.RegisterRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.auth.LoginChallengeResponse
import ltd.guimc.web.altget.entity.response.auth.LoginVerifyResponse
import ltd.guimc.web.altget.enum.EnumOauthUsage
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserDetailsService
import ltd.guimc.web.altget.service.user.UserOauthService
import ltd.guimc.web.altget.service.user.UserOperationService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
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
import java.util.*

@RestController("/api/auth")
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
    private val userOperationService: UserOperationService
) {
    // <editor-fold desc="用户注册">
    @PostMapping("/register")
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

    @GetMapping("/activate")
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

    // <editor-fold desc="用户账号密码登录">
    @GetMapping("/password/challenge")
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

    @PostMapping("/password/token")
    fun passwordVerify(@RequestBody request: LoginVerifyRequest): ResponseBase<LoginVerifyResponse> {
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
            val username = serverSession.getUserID().toIntOrNull() ?: return ResponseBase(400, "服务器 SRP Session 异常")
            val coreAuthEntity = coreAuthService.getByUsername(username.toString()) ?: return ResponseBase(400, "服务器 SRP Session 异常")
            val userDetails = userDetailsService.getById(coreAuthEntity.userId) ?: return ResponseBase(400, "服务器 SRP Session 异常")
            if (userDetails.userRole == EnumUserRole.UNVERIFY) return ResponseBase(400, "账号未激活，请先激活账号")
            if (userDetails.userRole == EnumUserRole.BANNED) return ResponseBase(400, "账号已被封禁，请联系管理员")
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
    @GetMapping("/github")
    fun githubOAuth(response: HttpServletResponse): ResponseBase<String> {
        val clientId = siteProperities.githubClientId
        val redirectUri = "https://${siteProperities.domain}/github-callback"
        val state = UUID.randomUUID().toString()
        // 将 state 存储在 Redis 中，设置过期时间为 10 分钟
        objectRedisTemplate.opsForValue().set("oauth:github:state:$state", EnumOauthUsage.LOGIN, Duration.ofMinutes(10))
        val oauthUrl = UrlBuilder.of("https://github.com/login/oauth/authorize")
            .addQuery("client_id", clientId)
            .addQuery("redirect_uri", redirectUri)
            .addQuery("state", state)
            .addQuery("scope", "user")
            .build()
        response.sendRedirect(oauthUrl)
        return ResponseBase(200, "OK")
    }

    @GetMapping("/github/state")
    fun getStateUsage(state: String): ResponseBase<String> {
        return ResponseBase(if ((objectRedisTemplate.opsForValue().get("oauth:github:state:$state") as? EnumOauthUsage
                ?: return ResponseBase(400, "无效的 state")) == EnumOauthUsage.LOGIN) "login" else "bind")
    }

    @GetMapping("/github/login")
    fun processLoginState(code: String, state: String): ResponseBase<String> {
        val storedState = objectRedisTemplate.opsForValue().get("oauth:github:state:$state") as? EnumOauthUsage ?: return ResponseBase(400, "无效的 state")
        objectRedisTemplate.delete("oauth:github:state:$state")
        if (storedState != EnumOauthUsage.LOGIN) {
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
        val jwtToken = jwtTokenComponent.generateLoginSession(userId)
        userOperationService.log(userId, EnumUserOperation.LOGIN, "Github OAuth 登录")
        return ResponseBase(jwtToken)
    }

    @GetMapping("/github/bind")
    fun processBindState(code: String, state: String, @CurrentUserId userId: Int?): ResponseBase<String> {
        val storedState = objectRedisTemplate.opsForValue().get("oauth:github:state:$state") as? EnumOauthUsage ?: return ResponseBase(400, "无效的 state")
        objectRedisTemplate.delete("oauth:github:state:$state")
        if (storedState != EnumOauthUsage.BIND) {
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