package ltd.guimc.web.altget.service.admin

import ltd.guimc.web.altget.component.AdminBadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdminSortTest {
    @Test
    fun `resolves public sort fields to fixed database columns`() {
        val spec = AdminSort.resolve(AdminList.OXAPAY_RECHARGES, "usdAmount", "asc")

        assertEquals("usd_amount", spec.column)
        assertEquals(true, spec.ascending)
        assertEquals("id", spec.tieBreakerColumn)
    }

    @Test
    fun `rejects fields outside the list whitelist`() {
        assertThrows(AdminBadRequestException::class.java) {
            AdminSort.resolve(AdminList.TOKENS, "id desc, (select 1)", "desc")
        }
    }

    @Test
    fun `rejects invalid sort direction`() {
        assertThrows(AdminBadRequestException::class.java) {
            AdminSort.resolve(AdminList.USERS, "id", "DROP")
        }
    }
}
