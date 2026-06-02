package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.RealIP
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.service.pow.PoWTaskService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PoWController(private val poWTaskService: PoWTaskService) {
    @GetMapping("/api/pow")
    fun createNewPowTask(target: String, @RealIP realIp: String): ResponseBase<PoWTaskService.PoWTask> {
        return ResponseBase(poWTaskService.createTask(target, realIp))
    }
}