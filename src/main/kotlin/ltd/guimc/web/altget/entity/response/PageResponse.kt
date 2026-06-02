package ltd.guimc.web.altget.entity.response

data class PageResponse<T>(
    val records: List<T>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)