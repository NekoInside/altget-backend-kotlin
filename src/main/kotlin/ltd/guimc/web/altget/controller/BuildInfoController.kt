package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.component.BuildInfo
import ltd.guimc.web.altget.entity.response.ResponseBase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BuildInfoController(
    private val buildInfo: BuildInfo
) {

    /**
     * Returns build metadata including git commit hash, branch name,
     * build time, Kotlin and Spring Boot versions.
     *
     * Example response:
     * ```json
     * {
     *   "code": 0,
     *   "message": "success",
     *   "data": {
     *     "git.commit.id": "a1b2c3d4",
     *     "git.branch": "main",
     *     "build.time": "2026-06-14T16:30:00Z",
     *     "build.version": "0.0.1-SNAPSHOT",
     *     ...
     *   }
     * }
     * ```
     */
    @GetMapping("/api/build-info")
    fun getBuildInfo(): ResponseBase<Map<String, String>> {
        return ResponseBase(buildInfo.toMap())
    }
}
