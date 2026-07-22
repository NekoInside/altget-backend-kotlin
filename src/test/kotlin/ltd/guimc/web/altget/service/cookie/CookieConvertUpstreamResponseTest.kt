package ltd.guimc.web.altget.service.cookie

import cn.hutool.json.JSONUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CookieConvertUpstreamResponseTest {
    @Test
    fun `parses successful Netscape cookie response without changing data`() {
        val cookie = "# Netscape HTTP Cookie File\n.example.com\tTRUE\t/\tFALSE\t0\tsession\tvalue"
        val response = CookieConvertUpstreamResponse.parse(
            JSONUtil.toJsonStr(mapOf("code" to 0, "message" to "success", "data" to cookie)),
        )

        assertEquals(0, response?.code)
        assertEquals("success", response?.message)
        assertEquals(cookie, response?.cookie)
    }

    @Test
    fun `parses upstream error message`() {
        val response = CookieConvertUpstreamResponse.parse("""{"code":1001,"message":"账号或密码错误","data":null}""")

        assertEquals(1001, response?.code)
        assertEquals("账号或密码错误", response?.message)
        assertNull(response?.cookie)
    }

    @Test
    fun `rejects malformed upstream response`() {
        assertNull(CookieConvertUpstreamResponse.parse("not json"))
        assertNull(CookieConvertUpstreamResponse.parse("""{"message":"missing code"}"""))
    }
}
