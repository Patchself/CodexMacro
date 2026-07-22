package com.patchself.codexmacro.protocol

data class ThreadLight(
    val color: Long = 0,
    val brightness: Float = 0f,
    val effect: Int = 0,
    val speed: Float = 0f,
    val syncKeysLighting: Boolean = false,
    val syncAmbientLighting: Boolean = false,
)

data class LightingSide(
    val color: Long = 0,
    val brightness: Float = 0f,
    val effect: Int = 0,
    val speed: Float = 0f,
    val magic: Float = 0f,
)

data class DeviceStatus(
    val battery: Int,
    val isCharging: Boolean,
)

enum class ControllerPhase {
    STOPPED,
    STARTING,
    ADVERTISING,
    CONNECTED,
    ERROR,
    UNSUPPORTED,
}

data class ControllerState(
    val phase: ControllerPhase = ControllerPhase.STOPPED,
    val hostName: String? = null,
    val battery: Int = 100,
    val isCharging: Boolean = false,
    val threads: List<ThreadLight> = List(6) { ThreadLight() },
    val ambient: LightingSide = LightingSide(),
    val keys: LightingSide = LightingSide(),
    val message: String? = null,
) {
    val isConnected: Boolean
        get() = phase == ControllerPhase.CONNECTED

    val isRunning: Boolean
        get() = phase != ControllerPhase.STOPPED &&
            phase != ControllerPhase.ERROR &&
            phase != ControllerPhase.UNSUPPORTED
}
