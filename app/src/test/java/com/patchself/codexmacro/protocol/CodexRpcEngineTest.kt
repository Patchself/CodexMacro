package com.patchself.codexmacro.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexRpcEngineTest {
    private val threads = MutableList(6) { ThreadLight() }
    private var ambient = LightingSide()
    private var keys = LightingSide()

    private val engine = CodexRpcEngine(
        statusProvider = { DeviceStatus(73, true) },
        layerProvider = { 4 },
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
        val response = JSONObject(
            requireNotNull(engine.handle(JSONObject("""{"method":"device.status","id":9}"""))),
        )
        val result = response.getJSONObject("result")

        assertEquals(9, response.getInt("id"))
        assertEquals(73, result.getInt("battery"))
        assertTrue(result.getBoolean("is_charging"))
        assertEquals(4, result.getInt("layer_index"))
        assertEquals(CodexRpcEngine.firmwareVersion, result.getString("version"))
    }

    @Test
    fun threadStatusPreservesFieldsOmittedByPartialUpdate() {
        threads[2] = ThreadLight(color = 0x112233, brightness = 0.4f, effect = "off", speed = 2f)

        engine.handle(
            JSONObject("""{"method":"v.oai.thstatus","params":[{"id":2,"b":0.9,"e":"breath"}],"id":3}"""),
        )

        assertEquals(0x112233, threads[2].color)
        assertEquals(0.9f, threads[2].brightness)
        assertEquals("breath", threads[2].effect)
        assertEquals(2f, threads[2].speed)
    }

    @Test
    fun unknownMethodReturnsJsonRpcError() {
        val response = JSONObject(
            requireNotNull(engine.handle(JSONObject("""{"method":"unknown.method","id":"rpc-1"}"""))),
        )

        assertEquals("rpc-1", response.getString("id"))
        assertEquals(-32601, response.getJSONObject("error").getInt("code"))
    }
}
