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
    ADMIN_BAN(6, "管理员封禁"),
    ADMIN_UNBAN(7, "管理员解封"),
    ADMIN_CREDIT_ADD(8, "管理员增加余额"),
    ADMIN_CREDIT_SUBTRACT(9, "管理员扣除余额"),
    ADMIN_TOKEN_GENERATE(10, "管理员生成Token"),
    API_KEY_NEW(11, "新建API密钥"),
    API_KEY_ROTATE(12, "轮换API密钥"),
}