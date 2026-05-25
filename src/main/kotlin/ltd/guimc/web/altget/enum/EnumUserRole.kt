package ltd.guimc.web.altget.enum

import com.baomidou.mybatisplus.annotation.EnumValue

enum class EnumUserRole(
    @EnumValue
    var role: Int,

    var roleName: String,
) {
    BANNED(-2, "已封禁"),
    UNVERIFY(-1, "未验证"),
    VERIFY(0, "已验证"),
    ADMIN(1, "管理员"),
}