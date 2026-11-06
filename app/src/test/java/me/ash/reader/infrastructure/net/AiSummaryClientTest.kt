package me.ash.reader.infrastructure.net

import me.ash.reader.infrastructure.net.AiSummaryClient.Companion.sseDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiSummaryClientTest {

    @Test
    fun testSseDelta() {
        assertEquals("Hel", sseDelta("data: " + DELTA_JSON))
        // z.ai / GLM style chunks carry the role on the first delta
        assertEquals(
            "Read",
            sseDelta("""data: {"choices":[{"index":0,"delta":{"role":"assistant","content":"Read"}}]}"""),
        )
        assertEquals(" ", sseDelta("data: " + DELTA_JSON.replace("Hel", " ")))
        assertNull(sseDelta("data: [DONE]"))
        assertNull(sseDelta(""))
        assertNull(sseDelta(": keep-alive"))
        assertNull(sseDelta("""data: {"choices":[{"delta":{},"finish_reason":"stop"}]}"""))
        assertNull(sseDelta("""data: {"error":{"message":"bad key"}}"""))
    }

    private companion object {
        const val DELTA_JSON = """{"choices":[{"index":0,"delta":{"content":"Hel"}}]}"""
    }
}
