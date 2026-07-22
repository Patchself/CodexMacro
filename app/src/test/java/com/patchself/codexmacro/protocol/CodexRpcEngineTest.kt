package com.patchself.codexmacro.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexRpcEngineTest {
    private val threads = MutableList(6) { ThreadLight() }
    private var ambient = LightingSide()
    private var keys = LightingSide()

    private val engine = CodexRpcEngine(
        statusProvider = { DeviceStatus(73, true) },
        layerProvider = { 0 },
        threadLightProvider = { threads[it] },
        ambientProvider = { ambient },
        keysProvider = { keys },
        onThreadLights = { updates -> updates.forEach { (id, light) -> threads[id] = light } },
        onLightingConfig = { newAmbient, newKeys ->
            if (newAmbient != null) ambient = newAmbient
            if (newKeys != null) keys = newKeys
        },
    )

    @Test
    fun deviceStatusReturnsBatteryAndChargingState() {
        val response = parseObject(
            requireNotNull(engine.handle(parseObject("""{"method":"device.status","id":9}"""))),
        )
        val result = response.getValue("result").jsonObject

        assertEquals(9, response.getValue("id").jsonPrimitive.int)
        assertEquals(73, result.getValue("battery").jsonPrimitive.int)
        assertTrue(result.getValue("is_charging").jsonPrimitive.content.toBoolean())
        assertEquals(0, result.getValue("layer_index").jsonPrimitive.int)
        assertEquals(CodexRpcEngine.firmwareVersion, result.getValue("version").jsonPrimitive.content)
    }

    @Test
    fun threadStatusPreservesFieldsOmittedByPartialUpdate() {
        threads[2] = ThreadLight(
            color = 0x112233,
            brightness = 0.4f,
            effect = 0,
            speed = 0.2f,
            syncAmbientLighting = true,
        )

        engine.handle(
            parseObject("""{"method":"v.oai.thstatus","params":[{"id":2,"b":0.9,"e":4,"sk":1}],"id":3}"""),
        )

        assertEquals(0x112233, threads[2].color)
        assertEquals(0.9f, threads[2].brightness)
        assertEquals(4, threads[2].effect)
        assertEquals(0.2f, threads[2].speed)
        assertTrue(threads[2].syncKeysLighting)
        assertTrue(threads[2].syncAmbientLighting)
    }

    @Test
    fun lightingConfigParsesNumericEffectsAndMagic() {
        engine.handle(
            parseObject(
                """{"method":"v.oai.rgbcfg","params":{"ambient":{"e":2,"b":0.7,"s":0.4,"m":0.3,"c":16711731},"keys":{"e":0,"b":0,"s":0,"m":0,"c":0}},"id":8}""",
            ),
        )

        assertEquals(2, ambient.effect)
        assertEquals(0.3f, ambient.magic)
        assertEquals(16711731, ambient.color)
        assertEquals(0, keys.effect)
    }

    @Test
    fun unknownMethodReturnsJsonRpcError() {
        val response = parseObject(
            requireNotNull(engine.handle(parseObject("""{"method":"unknown.method","id":"rpc-1"}"""))),
        )

        assertEquals("rpc-1", response.getValue("id").jsonPrimitive.content)
        assertEquals(-32601, response.getValue("error").jsonObject.getValue("code").jsonPrimitive.int)
    }

    private fun parseObject(value: String) = Json.parseToJsonElement(value).jsonObject
}
