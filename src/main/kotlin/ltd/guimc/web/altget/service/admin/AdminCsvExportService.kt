package ltd.guimc.web.altget.service.admin

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/** Writes Excel-friendly UTF-8 CSV without exposing formula-injection values. */
@org.springframework.stereotype.Service
class AdminCsvExportService {
    fun write(
        output: OutputStream,
        headers: List<String>,
        rows: Sequence<List<Any?>>,
    ) {
        output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
            writer.write(headers.joinToString(",", transform = ::escape))
            writer.write("\r\n")
            rows.forEach { row ->
                writer.write(row.joinToString(",", transform = { escape(it?.toString().orEmpty()) }))
                writer.write("\r\n")
            }
            writer.flush()
        }
    }

    private fun escape(value: String): String {
        val safeValue = if (value.firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value
        return if (safeValue.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"${safeValue.replace("\"", "\"\"")}\""
        } else {
            safeValue
        }
    }

    companion object {
        const val MAX_EXPORT_RECORDS = 10_000L
    }
}
