package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.component.GeetestVerifyComponent
import ltd.guimc.web.altget.entity.request.CaptchaRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("/api/alt")
class AltController(private val geetestVerifyComponent: GeetestVerifyComponent) {
    @GetMapping("/fetch")
    fun apiFetch(@RequestParam(required = false) userApiKey: String?, paid: Boolean = false): ResponseBase<String> {
        // TODO
        return ResponseBase("")
    }

    @PostMapping("/fetch")
    fun webFetch(@RequestBody request: CaptchaRequest): ResponseBase<String> {
        if (!geetestVerifyComponent.verify(request)) {
            return ResponseBase(1, "验证码错误")
        }
        // TODO
        return ResponseBase("")
    }
}