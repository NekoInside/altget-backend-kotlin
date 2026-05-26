package ltd.guimc.web.altget.enum

import com.baomidou.mybatisplus.annotation.EnumValue

enum class EnumUserOperation(
    @EnumValue val value: Int,
    val desc: String
) {
    LOGIN(0, "登录"),
    WEB_FETCH(1, "网页获取小号"),
    API_FETCH(2, "API获取小号"),
    PAID_API_FETCH(3, "付费API获取小号"),
    PASSWORD_RESET(4, "密码重置"),
    OAUTH_BIND(5, "OAuth绑定"),
}