package ltd.guimc.web.altget.component

import cn.hutool.core.net.url.UrlBuilder
import ltd.guimc.web.altget.config.CaptchaProperties
import ltd.guimc.web.altget.entity.request.CaptchaRequest
import ltd.guimc.web.altget.utils.HexUtils
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class GeetestVerifyComponent(
    private val captchaProperties: CaptchaProperties
) {
    val httpClient = HttpClient.newHttpClient()

    fun verify(request: CaptchaRequest): Boolean {
        val geetest = captchaProperties.geetest
        val key = geetest.key
        val sign = HexUtils.hmacSha256(key, request.lotNumber)
        val verifyUrl = UrlBuilder.of(geetest.apiServer)
            .addQuery("captcha_id", request.captchaId)
            .addQuery("captcha_output", request.captchaOutput)
            .addQuery("gen_time", request.genTime)
            .addQuery("lot_number", request.lotNumber)
            .addQuery("pass_token", request.passToken)
            .addQuery("sign_token", sign)
            .build()
            .toString()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(verifyUrl))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return response.statusCode() == 200 && response.body().contains("success")
    }
}