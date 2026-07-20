package com.patchself.codexmacro.protocol

import org.json.JSONArray
import org.json.JSONObject

class CodexRpcEngine(
    private val statusProvider: () -> DeviceStatus,
    private val layerProvider: () -> Int,
    private val threadLightProvider: (Int) -> ThreadLight,
    private val ambientProvider: () -> LightingSide,
    private val keysProvider: () -> LightingSide,
    private val onThreadLights: (List<Pair<Int, ThreadLight>>) -> Unit,
    private val onLightingConfig: (LightingSide?, LightingSide?) -> Unit,
) {
    fun handle(request: JSONObject): String? {
        val method = request.optString("method")
        val id = request.opt("id")
        return when (method) {
            "sys.version" -> response(id, JSONObject().put("version", firmwareVersion))
            "device.status" -> {
                val status = statusProvider()
                response(
                    id,
                    JSONObject()
                        .put("version", firmwareVersion)
                        .put("profile_index", 0)
                        .put("layer_index", layerProvider().coerceIn(1, 6))
                        .put("battery", status.battery)
                        .put("is_charging", status.isCharging),
                )
            }
            "v.oai.thstatus" -> {
                parseThreadLights(request.optJSONArray("params"))
                success(id)
            }
            "v.oai.rgbcfg" -> {
                parseLightingConfig(request.optJSONObject("params"))
                success(id)
            }
            "lights.preview", "host.focused_app" -> success(id)
            else -> error(id, -32601, "Method not found")
        }
    }

    private fun parseThreadLights(values: JSONArray?) {
        if (values == null) return
        val updates = buildList {
            repeat(values.length()) { index ->
                val value = values.optJSONObject(index) ?: return@repeat
                val id = value.optInt("id", -1)
                if (id !in 0..5) return@repeat
                val current = threadLightProvider(id)
                add(
                    id to ThreadLight(
                        color = if (value.has("c")) value.optLong("c") else current.color,
                        brightness = if (value.has("b")) value.optDouble("b").toFloat() else current.brightness,
                        effect = if (value.has("e")) value.optString("e") else current.effect,
                        speed = if (value.has("s")) value.optDouble("s").toFloat() else current.speed,
                    ),
                )
            }
        }
        onThreadLights(updates)
    }

    private fun parseLightingConfig(config: JSONObject?) {
        if (config == null) return
        onLightingConfig(
            config.optJSONObject("ambient")?.toLightingSide(ambientProvider()),
            config.optJSONObject("keys")?.toLightingSide(keysProvider()),
        )
    }

    private fun JSONObject.toLightingSide(current: LightingSide) = LightingSide(
        color = if (has("c")) optLong("c") else current.color,
        brightness = if (has("b")) optDouble("b").toFloat() else current.brightness,
        effect = if (has("e")) optString("e") else current.effect,
        speed = if (has("s")) optDouble("s").toFloat() else current.speed,
    )

    private fun success(id: Any?) = response(id, JSONObject().put("ok", true))

    private fun response(id: Any?, result: JSONObject): String = JSONObject()
        .put("id", id ?: JSONObject.NULL)
        .put("result", result)
        .toString()

    private fun error(id: Any?, code: Int, message: String): String = JSONObject()
        .put("id", id ?: JSONObject.NULL)
        .put("error", JSONObject().put("code", code).put("message", message))
        .toString()

    companion object {
        const val firmwareVersion = "0.1.0-android"
    }
}
