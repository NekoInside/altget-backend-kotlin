package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.response.ResponseBase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("/api/social")
class SocialController {
    // <editor-fold desc="Discord">
    @GetMapping("/discord/redirect")
    fun discordRedirect(): ResponseBase<String> {
        // TODO
        return ResponseBase("")
    }

    @GetMapping("/discord/token")
    fun diccordBind(@CurrentUserId userId: Int?): ResponseBase<String> {
        // TODO
        return ResponseBase("")
    }
    // </editor-fold>
}