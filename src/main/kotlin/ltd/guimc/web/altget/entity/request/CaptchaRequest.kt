package ltd.guimc.web.altget.entity.request

open class CaptchaRequest(
    open var captchaId: String = "",
    open var captchaOutput: String = "",
    open var genTime: String = "",
    open var lotNumber: String = "",
    open var passToken: String = ""
)