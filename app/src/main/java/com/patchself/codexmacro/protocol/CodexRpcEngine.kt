package com.patchself.codexmacro.protocol

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class CodexRpcEngine(
    private val statusProvider: () -> DeviceStatus,
    private val layerProvider: () -> Int,
    private val threadLightProvider: (Int) -> ThreadLight,
    private val ambientProvider: () -> LightingSide,
    private val keysProvider: () -> LightingSide,
    private val onThreadLights: (List<Pair<Int, ThreadLight>>) -> Unit,
    private val onLightingConfig: (LightingSide?, LightingSide?) -> Unit,
) {
    fun handle(request: JsonObject): String? {
        val method = (request["method"] as? JsonPrimitive)?.contentOrNull
        val id = request["id"] ?: JsonNull
        return when (method) {
            "sys.version" -> response(id, buildJsonObject { put("version", firmwareVersion) })
            "device.status" -> {
                val status = statusProvider()
                response(
                    id,
                    buildJsonObject {
                        put("version", firmwareVersion)
                        put("profile_index", 0)
                        put("layer_index", layerProvider().coerceIn(0, 5))
                        put("battery", status.battery)
                        put("is_charging", status.isCharging)
                    },
                )
            }
            "v.oai.thstatus" -> {
                parseThreadLights(request["params"] as? JsonArray)
                success(id)
            }
            "v.oai.rgbcfg" -> {
                parseLightingConfig(request["params"] as? JsonObject)
                success(id)
            }
            "lights.preview", "host.focused_app" -> success(id)
            else -> error(id, -32601, "Method not found")
        }
    }

    private fun parseThreadLights(values: JsonArray?) {
        if (values == null) return
        val updates = buildList {
            values.forEach { element ->
                val value = element as? JsonObject ?: return@forEach
                val id = (value["id"] as? JsonPrimitive)?.intOrNull ?: return@forEach
                if (id !in 0..5) return@forEach
                val current = threadLightProvider(id)
                add(
                    id to ThreadLight(
                        color = value.longOr("c", current.color),
                        brightness = value.floatOr("b", current.brightness),
                        effect = value.intOr("e", current.effect),
                        speed = value.floatOr("s", current.speed),
                        syncKeysLighting = value.booleanFlagOr("sk", current.syncKeysLighting),
                        syncAmbientLighting = value.booleanFlagOr("sa", current.syncAmbientLighting),
                    ),
                )
            }
        }
        onThreadLights(updates)
    }

    private fun parseLightingConfig(config: JsonObject?) {
        if (config == null) return
        onLightingConfig(
            (config["ambient"] as? JsonObject)?.toLightingSide(ambientProvider()),
            (config["keys"] as? JsonObject)?.toLightingSide(keysProvider()),
        )
    }

    private fun JsonObject.toLightingSide(current: LightingSide) = LightingSide(
        color = longOr("c", current.color),
        brightness = floatOr("b", current.brightness),
        effect = intOr("e", current.effect),
        speed = floatOr("s", current.speed),
        magic = floatOr("m", current.magic),
    )

    private fun JsonObject.longOr(key: String, fallback: Long): Long =
        (this[key] as? JsonPrimitive)?.longOrNull ?: fallback

    private fun JsonObject.floatOr(key: String, fallback: Float): Float =
        (this[key] as? JsonPrimitive)?.floatOrNull ?: fallback

    private fun JsonObject.intOr(key: String, fallback: Int): Int =
        (this[key] as? JsonPrimitive)?.intOrNull ?: fallback

    private fun JsonObject.booleanFlagOr(key: String, fallback: Boolean): Boolean =
        (this[key] as? JsonPrimitive)?.intOrNull?.let { it != 0 } ?: fallback

    private fun success(id: JsonElement) = response(id, buildJsonObject { put("ok", true) })

    private fun response(id: JsonElement, result: JsonObject): String = buildJsonObject {
        put("id", id)
        put("result", result)
    }.toString()

    private fun error(id: JsonElement, code: Int, message: String): String = buildJsonObject {
        put("id", id)
        put("error", buildJsonObject {
            put("code", code)
            put("message", message)
        })
    }.toString()

    companion object {
        const val firmwareVersion = "0.1.0-android"
    }
}
