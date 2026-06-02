package ltd.guimc.web.altget.entity.response.alt

/**
 * Response DTO for alt consumption chart data.
 * Each entry represents the consumption count for a given hour slot and channel.
 */
data class AltConsumptionResponse(
    /** Source channel (e.g., "default", "pre-processed") */
    val channel: String,
    /** Start of the hour time slot (ISO-8601 string) */
    val hourSlot: String,
    /** Number of alts consumed in this hour slot */
    val consumedCount: Int
)
