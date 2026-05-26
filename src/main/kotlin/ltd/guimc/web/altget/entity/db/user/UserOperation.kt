package ltd.guimc.web.altget.entity.db.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import ltd.guimc.web.altget.enum.EnumUserOperation
import java.time.LocalDateTime

@TableName
class UserOperation {
    @TableId(type = IdType.ASSIGN_UUID)
    var operationId: String? = null

    var operationTime: LocalDateTime = LocalDateTime.now()

    var userId: Int? = null

    var username: String? = null

    var operation: EnumUserOperation? = null

    var description: String? = null
}