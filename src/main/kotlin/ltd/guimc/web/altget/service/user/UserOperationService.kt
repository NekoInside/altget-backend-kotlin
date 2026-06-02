package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserOperation
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.mapper.db.user.UserOperationMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserOperationService(private val coreAuthService: CoreAuthService) : ServiceImpl<UserOperationMapper, UserOperation>(), IPageService<UserOperation> {
    fun log(userId: Int, operation: EnumUserOperation, description: String) {
        coreAuthService.getById(userId)?.let {
            save(UserOperation().apply {
                this.userId = userId
                this.username = it.username
                this.operation = operation
                this.description = description
                this.operationTime = LocalDateTime.now()
            })
        }
    }

    fun removeByUserId(userId: Int) {
        remove(query().eq("user_id", userId))
    }
}