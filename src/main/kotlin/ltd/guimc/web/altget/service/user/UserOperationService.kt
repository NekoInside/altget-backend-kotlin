package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserOperation
import ltd.guimc.web.altget.enum.EnumUserOperation
import ltd.guimc.web.altget.mapper.db.user.CoreAuthMapper
import ltd.guimc.web.altget.mapper.db.user.UserOperationMapper
import ltd.guimc.web.altget.service.geolocation.GeolocationService
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class UserOperationService(
    private val coreAuthMapper: CoreAuthMapper,
    private val geolocationService: GeolocationService
) : ServiceImpl<UserOperationMapper, UserOperation>(), IPageService<UserOperation> {

    private val logExecutor = Executors.newFixedThreadPool(2)

    fun log(userId: Int, operation: EnumUserOperation, description: String, ip: String) {
        CompletableFuture.runAsync({
            val geoip = geolocationService.formatLocationMessage(geolocationService.getGeolocation(ip))

            coreAuthMapper.selectById(userId)?.let {
                save(UserOperation().apply {
                    this.userId = userId
                    this.username = it.username
                    this.operation = operation
                    this.description = description
                    this.operationTime = LocalDateTime.now()
                    this.ip = ip
                    this.geoip = geoip
                })
            }
        }, logExecutor)
    }

    fun logAnonymous(operation: EnumUserOperation, description: String, ip: String) {
        CompletableFuture.runAsync({
            val geoip = geolocationService.formatLocationMessage(geolocationService.getGeolocation(ip))

            save(UserOperation().apply {
                this.userId = -1
                this.username = "Anonymous"
                this.operation = operation
                this.description = description
                this.operationTime = LocalDateTime.now()
                this.ip = ip
                this.geoip = geoip
            })
        }, logExecutor)
    }

    fun removeByUserId(userId: Int) {
        remove(QueryWrapper<UserOperation>().eq("user_id", userId))
    }
}
