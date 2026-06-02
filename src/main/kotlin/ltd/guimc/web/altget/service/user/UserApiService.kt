package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserApi
import ltd.guimc.web.altget.mapper.db.user.UserApiMapper
import org.springframework.stereotype.Service

@Service
class UserApiService : ServiceImpl<UserApiMapper, UserApi>() {
    fun getByApiKey(key: String): UserApi {
        if (key.isBlank()) {
            throw IllegalArgumentException("API key cannot be blank")
        }
        try {
            val userApi = query()
                .eq("api_key", key)
                .one()
            if (userApi == null) {
                throw IllegalArgumentException("Invalid API key")
            }
            return userApi
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid API key")
        }
    }
}