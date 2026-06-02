package ltd.guimc.web.altget.mapper.db.alt

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.alt.AltConsumptionRecord
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDateTime

@Mapper
interface AltConsumptionRecordMapper : BaseMapper<AltConsumptionRecord> {

    /**
     * Atomically increment the consumed count for a given (channel, hourSlot) pair.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE (MariaDB/MySQL syntax).
     */
    @Insert("""
        INSERT INTO alt_consumption_record (channel, hour_slot, consumed_count)
        VALUES (#{channel}, #{hourSlot}, #{count})
        ON DUPLICATE KEY UPDATE consumed_count = consumed_count + #{count}
    """)
    fun incrementConsumption(
        @Param("channel") channel: String,
        @Param("hourSlot") hourSlot: LocalDateTime,
        @Param("count") count: Int
    ): Int

    /**
     * Delete all consumption records whose [hourSlot] is before the given cutoff time.
     * @return Number of rows deleted
     */
    @Delete("DELETE FROM alt_consumption_record WHERE hour_slot < #{cutoff}")
    fun deleteOlderThan(@Param("cutoff") cutoff: LocalDateTime): Int
}
