package ltd.guimc.web.altget.entity.response.alt

/**
 * Response DTO for alt consumption chart data.
 *
 * [time] is the ordered list of bucket boundaries (ISO-8601 UTC strings), and
 * [values] holds one count-series per fetch source plus a combined "total" series.
 * Each series is aligned with [time] (same length, same order).
 */
data class AltConsumptionResponse(
    val time: List<String>,
    val values: Map<String, List<Int>>
)
