package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserOauth
import ltd.guimc.web.altget.mapper.db.user.UserOauthMapper
import org.springframework.stereotype.Service

@Service
class UserOauthService : ServiceImpl<UserOauthMapper, UserOauth>() {
    fun getUserIdByGithubId(githubId: String): Int? {
        val userOauth = query()
            .eq("github_id", githubId)
            .one()
        return userOauth?.userId
    }

    fun setGithubId(userId: Int, githubId: String) {
        val userOauth = query()
            .eq("user_id", userId)
            .one()
        if (userOauth == null) {
            save(UserOauth().apply {
                this.userId = userId
                this.githubId = githubId
            })
        } else {
            userOauth.githubId = githubId
            updateById(userOauth)
        }
    }
}