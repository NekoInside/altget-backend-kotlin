package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.user.UserInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController("/api/admin")
class AdminController {
    @GetMapping("/users")
    fun listUsers(@CurrentUserId userId: Int?, size: Int, page: Int): ResponseBase<List<UserInfo>> {
        // TODO
        return ResponseBase(emptyList())
    }

    @GetMapping("/user/{userId}")
    fun getUser(@PathVariable userId: Int?): ResponseBase<UserInfo> {
        // TODO
        return ResponseBase(500, "Not implemented yet")
    }
}