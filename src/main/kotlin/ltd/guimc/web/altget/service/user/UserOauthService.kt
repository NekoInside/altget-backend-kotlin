package ltd.guimc.web.altget.service.user

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.user.UserOauth
import ltd.guimc.web.altget.mapper.db.user.UserOauthMapper
import org.springframework.stereotype.Service

@Service
class UserOauthService : ServiceImpl<UserOauthMapper, UserOauth>() {
    fun getUserIdByGithubId(githubId: String): Int? {
        val userOauth = getOne(QueryWrapper<UserOauth>()
            .eq("github_id", githubId))
        return userOauth?.userId
    }

    fun setGithubId(userId: Int, githubId: String) {
        val userOauth = getOne(QueryWrapper<UserOauth>()
            .eq("user_id", userId))
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

    fun getUserIdByDiscordId(discordId: String): Int? {
        val userOauth = getOne(QueryWrapper<UserOauth>()
            .eq("discord_id", discordId))
        return userOauth?.userId
    }

    fun setDiscordId(userId: Int, discordId: String) {
        val userOauth = getOne(QueryWrapper<UserOauth>()
            .eq("user_id", userId))
        if (userOauth == null) {
            save(UserOauth().apply {
                this.userId = userId
                this.discordId = discordId
            })
        } else {
            userOauth.discordId = discordId
            updateById(userOauth)
        }
    }

    fun getDiscordIdByUserId(userId: Int): String? {
        val userOauth = getOne(QueryWrapper<UserOauth>()
            .eq("user_id", userId))
        return userOauth?.discordId
    }
}