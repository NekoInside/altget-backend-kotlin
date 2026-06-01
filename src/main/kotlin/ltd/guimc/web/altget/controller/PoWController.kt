package ltd.guimc.web.altget.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/api/pow")
class PoWController {
    @GetMapping
    fun createNewPowTask()
}