package ltd.guimc.web.altget.service.admin

import ltd.guimc.web.altget.entity.response.admin.AdminProfileResponse
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CurrentAdminService(
    private val coreAuthService: CoreAuthService,
    private val userDetailsService: UserDetailsService,
) {
    fun getProfile(userId: Int): AdminProfileResponse? {
        val auth = coreAuthService.getById(userId) ?: return null
        val details = userDetailsService.getById(userId) ?: return null
        val username = auth.username?.takeIf { it.isNotBlank() } ?: return null

        return AdminProfileResponse(
            id = userId.toLong(),
            username = username,
            // The current schema has no separate display-name or avatar columns.
            // Keep these nullable so the frontend can apply its documented fallback.
            displayName = null,
            email = auth.email,
            role = details.userRole.name,
            avatarUrl = null,
        )
    }
}
