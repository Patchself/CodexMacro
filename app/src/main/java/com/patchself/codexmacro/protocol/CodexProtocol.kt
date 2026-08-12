package com.patchself.codexmacro.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayOutputStream

object CodexProtocol {
    const val keyboardReportId = 1
    const val keyboardReportSize = 8
    const val reportId = 6
    const val reportBodySize = 63
    const val payloadSize = 61
    const val messageType = 2
    const val maxRpcSize = 4096
    const val effectOff = 0
    const val effectSolid = 1
    const val effectSnake = 2
    const val effectRainbow = 3
    const val effectBreath = 4
    const val effectGradient = 5
    const val effectShallowBreath = 6

    val reportMap = byteArrayOf(
        0x05, 0x01,
        0x09, 0x06,
        0xA1.toByte(), 0x01,
        0x85.toByte(), keyboardReportId.toByte(),
        0x05, 0x07,
        0x19, 0xE0.toByte(),
        0x29, 0xE7.toByte(),
        0x15, 0x00,
        0x25, 0x01,
        0x75, 0x01,
        0x95.toByte(), 0x08,
        0x81.toByte(), 0x02,
        0x95.toByte(), 0x01,
        0x75, 0x08,
        0x81.toByte(), 0x01,
        0x95.toByte(), 0x06,
        0x75, 0x08,
        0x15, 0x00,
        0x25, 0x65,
        0x05, 0x07,
        0x19, 0x00,
        0x29, 0x65,
        0x81.toByte(), 0x00,
        0xC0.toByte(),
        0x06, 0x00, 0xFF.toByte(),
        0x09, 0x01,
        0xA1.toByte(), 0x01,
        0x85.toByte(), reportId.toByte(),
        0x15, 0x00,
        0x26, 0xFF.toByte(), 0x00,
        0x75, 0x08,
        0x95.toByte(), 0x3F,
        0x09, 0x01,
        0x81.toByte(), 0x02,
        0x95.toByte(), 0x3F,
        0x09, 0x02,
        0x91.toByte(), 0x02,
        0xC0.toByte(),
    )

    fun frame(json: String): List<ByteArray> {
        val payload = "$json\n".toByteArray(Charsets.UTF_8)
        return payload.asList().chunked(payloadSize).map { chunk ->
            ByteArray(reportBodySize).also { report ->
                report[0] = messageType.toByte()
                report[1] = chunk.size.toByte()
                chunk.forEachIndexed { index, byte -> report[index + 2] = byte }
            }
        }
    }

    /** keyboardReport creates a standard six-key rollover keyboard input report. */
    fun keyboardReport(modifiers: Int, usage: Int): ByteArray =
        ByteArray(keyboardReportSize).also { report ->
            report[0] = (modifiers and 0xFF).toByte()
            report[2] = (usage and 0xFF).toByte()
        }
}

sealed interface DecodeResult {
    data object Incomplete : DecodeResult
    data class Complete(val json: JsonObject) : DecodeResult
    data class Invalid(val reason: String) : DecodeResult
}

class CodexFrameDecoder {
    private val buffer = ByteArrayOutputStream()

    fun reset() {
        buffer.reset()
    }

    fun consume(report: ByteArray): DecodeResult {
        if (report.size < 2) return DecodeResult.Invalid("report is too short")

        val offset = if (report.size >= 3 && report[0].toInt() == CodexProtocol.reportId) 1 else 0
        if (report.size < offset + 2) return DecodeResult.Invalid("report header is incomplete")
        if (report[offset].toInt() != CodexProtocol.messageType) {
            return DecodeResult.Invalid("unsupported message type")
        }

        val payloadLength = report[offset + 1].toInt() and 0xFF
        if (payloadLength > CodexProtocol.payloadSize || report.size < offset + 2 + payloadLength) {
            reset()
            return DecodeResult.Invalid("invalid payload length")
        }

        val payload = report.copyOfRange(offset + 2, offset + 2 + payloadLength)
        val prefix = "{\"method\"".toByteArray(Charsets.UTF_8)
        if (buffer.size() > 0 && payload.startsWith(prefix)) reset()

        val content = if (buffer.size() == 0) {
            val jsonStart = payload.indexOf('{'.code.toByte())
            if (jsonStart < 0) return DecodeResult.Invalid("missing JSON object")
            payload.copyOfRange(jsonStart, payload.size)
        } else {
            payload
        }
        buffer.write(content)
        if (buffer.size() > CodexProtocol.maxRpcSize) {
            reset()
            return DecodeResult.Invalid("RPC message exceeds limit")
        }

        val candidate = buffer.toByteArray().toString(Charsets.UTF_8).trim()
        if (!candidate.endsWith('}')) return DecodeResult.Incomplete

        return try {
            val json = Json.parseToJsonElement(candidate) as? JsonObject
            if (json == null) {
                reset()
                return DecodeResult.Invalid("JSON payload is not an object")
            }
            reset()
            DecodeResult.Complete(json)
        } catch (_: IllegalArgumentException) {
            reset()
            DecodeResult.Invalid("malformed JSON")
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
