package ltd.guimc.web.altget.entity.convert

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

@TableName
class ConvertTask {
    @TableId(type = IdType.ASSIGN_UUID)
    var taskId: String? = null

    var userId: Int? = null

    var userName: String? = null

    var password: String? = null

    var submitTime: Long? = null

    var assigned: Boolean? = null

    var finished: Boolean? = null

    var success: Boolean? = null

    var result: String? = null
}