package ltd.guimc.web.altget.service.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class AdminCsvExportServiceTest {
    @Test
    fun `writes utf8 bom and protects formula values`() {
        val output = ByteArrayOutputStream()

        AdminCsvExportService().write(
            output,
            listOf("name", "description"),
            sequenceOf(listOf("=SUM(A1)", "hello, \"admin\"")),
        )

        val bytes = output.toByteArray()
        assertEquals(listOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.take(3))
        val csv = bytes.toString(StandardCharsets.UTF_8)
        assertTrue(csv.contains("'=SUM(A1)"))
        assertTrue(csv.contains("\"hello, \"\"admin\"\"\""))
    }
}
