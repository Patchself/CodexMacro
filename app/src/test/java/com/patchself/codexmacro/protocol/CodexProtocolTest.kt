package com.patchself.codexmacro.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

class CodexProtocolTest {
    @Test
    fun frameDecoderReassemblesFragmentedJson() {
        val payload = """{"method":"device.status","params":{"value":"${"x".repeat(100)}"},"id":7}"""
        val decoder = CodexFrameDecoder()
        val reports = CodexProtocol.frame(payload)

        assertTrue(reports.size > 1)
        reports.dropLast(1).forEach { report ->
            assertEquals(DecodeResult.Incomplete, decoder.consume(report))
        }
        val complete = decoder.consume(reports.last()) as DecodeResult.Complete

        assertEquals("device.status", complete.json["method"]?.jsonPrimitive?.content)
        assertEquals(7, complete.json["id"]?.jsonPrimitive?.int)
    }

    @Test
    fun frameDecoderAcceptsExplicitReportId() {
        val body = CodexProtocol.frame("""{"method":"sys.version","id":1}""").single()
        val report = byteArrayOf(CodexProtocol.reportId.toByte()) + body

        val result = CodexFrameDecoder().consume(report) as DecodeResult.Complete

        assertEquals("sys.version", result.json["method"]?.jsonPrimitive?.content)
    }

    @Test
    fun frameDecoderRejectsInvalidPayloadLength() {
        val report = ByteArray(CodexProtocol.reportBodySize).apply {
            this[0] = CodexProtocol.messageType.toByte()
            this[1] = (CodexProtocol.payloadSize + 1).toByte()
        }

        val result = CodexFrameDecoder().consume(report)

        assertTrue(result is DecodeResult.Invalid)
    }

    @Test
    fun frameDecoderResynchronizesAtNewTopLevelRequest() {
        val decoder = CodexFrameDecoder()
        val partial = rawReport("""{"method":"stale"""")
        val replacement = CodexProtocol.frame("""{"method":"sys.version","id":4}""").single()

        assertEquals(DecodeResult.Incomplete, decoder.consume(partial))
        val complete = decoder.consume(replacement) as DecodeResult.Complete

        assertEquals("sys.version", complete.json["method"]?.jsonPrimitive?.content)
    }

    private fun rawReport(payload: String): ByteArray {
        val bytes = payload.toByteArray()
        return ByteArray(CodexProtocol.reportBodySize).apply {
            this[0] = CodexProtocol.messageType.toByte()
            this[1] = bytes.size.toByte()
            bytes.copyInto(this, destinationOffset = 2)
        }
    }
}
