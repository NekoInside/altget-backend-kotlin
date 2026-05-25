package ltd.guimc.web.altget.enum

import com.baomidou.mybatisplus.annotation.EnumValue

enum class EnumApiLimitLevel (
    @EnumValue
    var level: Int,
    var limitDau: Long
) {
    LEVEL_LOW(1, 50),
    LEVEL_MEDIUM(2, 25),
    LEVEL_HIGH(3, 0),
    LEVEL_UNLIMITED(-1, -1),
}