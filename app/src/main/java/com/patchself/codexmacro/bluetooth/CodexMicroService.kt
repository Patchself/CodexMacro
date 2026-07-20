package com.patchself.codexmacro.bluetooth

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.patchself.codexmacro.MainActivity
import com.patchself.codexmacro.R
import com.patchself.codexmacro.protocol.CodexFrameDecoder
import com.patchself.codexmacro.protocol.CodexProtocol
import com.patchself.codexmacro.protocol.CodexRpcEngine
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.protocol.DecodeResult
import com.patchself.codexmacro.protocol.DeviceStatus
import com.patchself.codexmacro.protocol.LightingSide
import com.patchself.codexmacro.protocol.ThreadLight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque
import java.util.UUID

@Suppress("MissingPermission", "DEPRECATION")
class CodexMicroService : Service() {
    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val decoder = CodexFrameDecoder()
    private val pendingReports = ArrayDeque<ByteArray>()
    private val pendingServices = ArrayDeque<BluetoothGattService>()
    private val preparedOutput = ByteArrayOutputStream()
    private val _state = MutableStateFlow(ControllerState())
    private val _settings = MutableStateFlow(ControllerSettings())

    val state: StateFlow<ControllerState> = _state.asStateFlow()
    val settings: StateFlow<ControllerSettings> = _settings.asStateFlow()

    private val bluetoothManager by lazy { getSystemService(BluetoothManager::class.java) }
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val preferences by lazy { getSharedPreferences(preferencesName, MODE_PRIVATE) }

    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null
    private var inputReport: BluetoothGattCharacteristic? = null
    private var outputReport: BluetoothGattCharacteristic? = null
    private var batteryLevel: BluetoothGattCharacteristic? = null
    private var inputNotificationsEnabled = false
    private var batteryNotificationsEnabled = false
    private var sendingReports = false
    private var controllerStarted = false
    private var advertising = false
    private var nameRecoveryPending = false
    private var foregroundActive = false

    private val rpcEngine by lazy {
        CodexRpcEngine(
            statusProvider = { DeviceStatus(_state.value.battery, _state.value.isCharging) },
            layerProvider = { _settings.value.activeLayer + 1 },
            threadLightProvider = { _state.value.threads[it] },
            ambientProvider = { _state.value.ambient },
            keysProvider = { _state.value.keys },
            onThreadLights = ::updateThreadLights,
            onLightingConfig = ::updateLightingConfig,
        )
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val percentage = (level * 100 / scale).coerceIn(0, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            _state.value = _state.value.copy(battery = percentage, isCharging = charging)
            notifyBatteryLevel()
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            advertising = true
            if (connectedDevice != null) {
                stopAdvertising()
                setPhase(
                    ControllerPhase.CONNECTED,
                    hostName = connectedDevice?.name ?: "macOS host",
                    message = "Codex Micro is connected",
                )
                return
            }
            setPhase(ControllerPhase.ADVERTISING, message = "Pair from macOS Bluetooth settings")
        }

        override fun onStartFailure(errorCode: Int) {
            fail("BLE advertising failed ($errorCode)")
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail("GATT service registration failed ($status)")
                return
            }
            addNextService()
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            handler.post {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> onHostConnected(device)
                    BluetoothProfile.STATE_DISCONNECTED -> onHostDisconnected(device)
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            sendReadResponse(device, requestId, offset, characteristicValue(characteristic))
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic === outputReport) {
                handleOutputWrite(device, requestId, preparedWrite, responseNeeded, offset, value)
                return
            }
            if (characteristic.uuid == protocolModeUuid || characteristic.uuid == hidControlPointUuid) {
                characteristic.value = value
                if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                return
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            if (execute && preparedOutput.size() > 0) processOutputReport(preparedOutput.toByteArray())
            preparedOutput.reset()
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor,
        ) {
            val value = when (descriptor.uuid) {
                clientConfigUuid -> when (descriptor.characteristic) {
                    inputReport -> if (inputNotificationsEnabled) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    batteryLevel -> if (batteryNotificationsEnabled) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    else -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                else -> descriptor.value ?: byteArrayOf()
            }
            sendReadResponse(device, requestId, offset, value)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (descriptor.uuid == clientConfigUuid && offset == 0 && !preparedWrite) {
                val enabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                when (descriptor.characteristic) {
                    inputReport -> inputNotificationsEnabled = enabled
                    batteryLevel -> batteryNotificationsEnabled = enabled
                }
                descriptor.value = value
                if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            } else if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        _settings.value = loadSettings()
        nameRecoveryPending = !_settings.value.stableConnection && recoverBluetoothName()
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null && !shouldAutoResume(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            actionStop -> stopController()
            else -> {
                startForeground(notificationId, buildNotification())
                foregroundActive = true
                startController()
            }
        }
        return if (_settings.value.autoResume) START_STICKY else START_NOT_STICKY
    }

    override fun onDestroy() {
        stopTransport()
        if (!_settings.value.stableConnection) restoreBluetoothName()
        unregisterReceiver(batteryReceiver)
        super.onDestroy()
    }

    fun sendKey(key: String, action: Int, agent: Int? = null) {
        if (!_state.value.isConnected) return
        val params = JSONObject().put("k", key).put("act", action)
        if (agent != null) params.put("ag", agent)
        sendJson(JSONObject().put("method", "v.oai.hid").put("params", params).toString())
    }

    fun sendJoystick(angle: Double, distance: Double) {
        if (!_state.value.isConnected) return
        val params = JSONObject().put("a", angle).put("d", distance)
        sendJson(JSONObject().put("method", "v.oai.rad").put("params", params).toString())
    }

    /** updateSettings persists controller options and applies safe connection-mode changes. */
    fun updateSettings(settings: ControllerSettings) {
        val previous = _settings.value
        val normalizedSettings = settings.copy(
            activeLayer = settings.activeLayer.coerceIn(0, CommandKeycap.layerCount - 1),
            layerKeycaps = CommandKeycap.normalizeLayers(settings.layerKeycaps),
        )
        _settings.value = normalizedSettings
        preferences.edit {
            putBoolean(stableConnectionKey, normalizedSettings.stableConnection)
            putBoolean(autoResumeKey, normalizedSettings.autoResume)
            putInt(activeLayerKey, normalizedSettings.activeLayer)
            putString(layerKeycapsKey, CommandKeycap.encodeLayers(normalizedSettings.layerKeycaps))
            remove(commandKeycapsKey)
        }
        if (previous.stableConnection && !normalizedSettings.stableConnection && !controllerStarted) {
            stopTransport()
            restoreBluetoothName()
            if (foregroundActive) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                foregroundActive = false
            }
            stopSelf()
        }
    }

    /** cycleLayer advances to the next persisted controller layer. */
    fun cycleLayer() {
        val nextLayer = (_settings.value.activeLayer + 1) % CommandKeycap.layerCount
        updateSettings(_settings.value.copy(activeLayer = nextLayer))
    }

    fun stopController() {
        controllerStarted = false
        preferences.edit { putBoolean(controllerRunningKey, false) }
        if (_settings.value.stableConnection) {
            stopAdvertising()
            setPhase(
                ControllerPhase.STOPPED,
                message = "Controller paused; stable connection remains active",
            )
            return
        }
        stopTransport()
        restoreBluetoothName()
        _state.value = ControllerState(
            battery = _state.value.battery,
            isCharging = _state.value.isCharging,
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundActive = false
        stopSelf()
    }

    private fun startController() {
        if (controllerStarted) return
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            fail("Bluetooth LE is not supported")
            return
        }
        if (!hasBluetoothPermissions()) {
            fail("Nearby devices permission is required")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            fail("Turn on Bluetooth before starting")
            return
        }
        if (bluetoothAdapter.bluetoothLeAdvertiser == null) {
            fail("This device does not support BLE peripheral advertising")
            return
        }

        controllerStarted = true
        preferences.edit { putBoolean(controllerRunningKey, true) }
        if (gattServer != null) {
            if (connectedDevice != null) {
                setPhase(ControllerPhase.CONNECTED, message = "Codex Micro is connected")
            } else {
                startAdvertising()
            }
            return
        }
        setPhase(ControllerPhase.STARTING, message = "Preparing BLE HID services")
        if (nameRecoveryPending || recoverBluetoothName()) {
            nameRecoveryPending = false
            handler.postDelayed({ setControllerBluetoothName(bluetoothAdapter) }, nameChangeDelayMs)
        } else {
            setControllerBluetoothName(bluetoothAdapter)
        }
    }

    private fun setControllerBluetoothName(bluetoothAdapter: BluetoothAdapter) {
        if (!controllerStarted) return
        if (!saveAndSetBluetoothName(bluetoothAdapter)) {
            fail("Unable to set Bluetooth name to Codex Micro")
            return
        }
        handler.postDelayed(::openGattServer, nameChangeDelayMs)
    }

    private fun openGattServer() {
        if (!controllerStarted) return
        val server = bluetoothManager.openGattServer(this, gattCallback)
        if (server == null) {
            fail("Unable to open GATT server")
            return
        }
        gattServer = server
        pendingServices.clear()
        pendingServices.addAll(createGattServices())
        addNextService()
    }

    private fun createGattServices(): List<BluetoothGattService> {
        val deviceInfo = BluetoothGattService(deviceInformationServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        deviceInfo.addCharacteristic(readCharacteristic(manufacturerNameUuid, "Work Louder".toByteArray()))
        deviceInfo.addCharacteristic(
            readCharacteristic(
                pnpIdUuid,
                byteArrayOf(0x02, 0x3A, 0x30, 0x60, 0x83.toByte(), 0x01, 0x01),
            ),
        )

        val hid = BluetoothGattService(hidServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        hid.addCharacteristic(readCharacteristic(hidInformationUuid, byteArrayOf(0x11, 0x01, 0x00, 0x01)))
        hid.addCharacteristic(readCharacteristic(reportMapUuid, CodexProtocol.reportMap))
        hid.addCharacteristic(
            BluetoothGattCharacteristic(
                hidControlPointUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED,
            ),
        )
        hid.addCharacteristic(
            BluetoothGattCharacteristic(
                protocolModeUuid,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE,
            ).apply { value = byteArrayOf(0x01) },
        )

        inputReport = BluetoothGattCharacteristic(
            reportUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED,
        ).apply {
            value = ByteArray(CodexProtocol.reportBodySize)
            addDescriptor(
                BluetoothGattDescriptor(
                    clientConfigUuid,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED,
                ),
            )
            addDescriptor(reportReferenceDescriptor(0x01))
        }
        outputReport = BluetoothGattCharacteristic(
            reportUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED,
        ).apply {
            value = ByteArray(CodexProtocol.reportBodySize)
            addDescriptor(reportReferenceDescriptor(0x02))
        }
        hid.addCharacteristic(inputReport)
        hid.addCharacteristic(outputReport)

        val battery = BluetoothGattService(batteryServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        batteryLevel = BluetoothGattCharacteristic(
            batteryLevelUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        ).apply {
            value = byteArrayOf(_state.value.battery.toByte())
            addDescriptor(
                BluetoothGattDescriptor(
                    clientConfigUuid,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
                ),
            )
            addDescriptor(
                BluetoothGattDescriptor(
                    characteristicPresentationFormatUuid,
                    BluetoothGattDescriptor.PERMISSION_READ,
                ).apply { value = byteArrayOf(0x04, 0x00, 0xAD.toByte(), 0x27, 0x01, 0x00, 0x00) },
            )
        }
        battery.addCharacteristic(batteryLevel)
        return listOf(deviceInfo, hid, battery)
    }

    private fun addNextService() {
        val service = pendingServices.pollFirst()
        if (service == null) {
            if (connectedDevice != null) {
                setPhase(
                    ControllerPhase.CONNECTED,
                    hostName = connectedDevice?.name ?: "macOS host",
                    message = "Codex Micro is connected",
                )
            } else {
                startAdvertising()
            }
            return
        }
        if (gattServer?.addService(service) != true) fail("Unable to add GATT service ${service.uuid}")
    }

    private fun startAdvertising() {
        if (advertising || connectedDevice != null) return
        val bluetoothAdapter = adapter ?: return fail("Bluetooth adapter is unavailable")
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            ?: return fail("BLE advertiser is unavailable")
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(hidServiceUuid))
            .setIncludeTxPowerLevel(false)
            .build()
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()
        advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)
    }

    private fun onHostConnected(device: BluetoothDevice) {
        connectedDevice = device
        decoder.reset()
        pendingReports.clear()
        sendingReports = false
        stopAdvertising()
        setPhase(
            ControllerPhase.CONNECTED,
            hostName = device.name ?: "macOS host",
            message = "Codex Micro is connected",
        )
    }

    private fun onHostDisconnected(device: BluetoothDevice) {
        if (connectedDevice?.address != device.address) return
        connectedDevice = null
        inputNotificationsEnabled = false
        batteryNotificationsEnabled = false
        decoder.reset()
        pendingReports.clear()
        sendingReports = false
        if (controllerStarted) startAdvertising()
    }

    private fun handleOutputWrite(
        device: BluetoothDevice,
        requestId: Int,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray,
    ) {
        var status = BluetoothGatt.GATT_SUCCESS
        if (preparedWrite) {
            if (offset != preparedOutput.size() || preparedOutput.size() + value.size > CodexProtocol.reportBodySize + 1) {
                status = BluetoothGatt.GATT_INVALID_OFFSET
            } else {
                preparedOutput.write(value)
            }
        } else if (offset != 0) {
            status = BluetoothGatt.GATT_INVALID_OFFSET
        } else {
            outputReport?.value = value
            processOutputReport(value)
        }
        if (responseNeeded) gattServer?.sendResponse(device, requestId, status, offset, value)
    }

    private fun processOutputReport(value: ByteArray) {
        when (val result = decoder.consume(value)) {
            is DecodeResult.Complete -> rpcEngine.handle(result.json)?.let(::sendJson)
            is DecodeResult.Invalid -> Log.w(logTag, result.reason)
            DecodeResult.Incomplete -> Unit
        }
    }

    private fun sendJson(json: String) {
        if (!_state.value.isConnected) return
        pendingReports.addAll(CodexProtocol.frame(json))
        if (!sendingReports) sendNextReport()
    }

    private fun sendNextReport() {
        val device = connectedDevice
        val characteristic = inputReport
        val report = pendingReports.pollFirst()
        if (device == null || characteristic == null || report == null || !inputNotificationsEnabled) {
            pendingReports.clear()
            sendingReports = false
            return
        }
        sendingReports = true
        characteristic.value = report
        notifyCharacteristic(device, characteristic, report)
        handler.postDelayed(::sendNextReport, reportDelayMs)
    }

    private fun notifyBatteryLevel() {
        val device = connectedDevice ?: return
        val characteristic = batteryLevel ?: return
        if (!batteryNotificationsEnabled) return
        val value = byteArrayOf(_state.value.battery.toByte())
        characteristic.value = value
        notifyCharacteristic(device, characteristic, value)
    }

    private fun notifyCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = gattServer?.notifyCharacteristicChanged(device, characteristic, false, value)
            if (status != BluetoothStatusCodes.SUCCESS) Log.w(logTag, "notification failed: $status")
        } else {
            gattServer?.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private fun updateThreadLights(updates: List<Pair<Int, ThreadLight>>) {
        if (updates.isEmpty()) return
        val threads = _state.value.threads.toMutableList()
        updates.forEach { (id, light) -> threads[id] = light }
        _state.value = _state.value.copy(threads = threads)
    }

    private fun updateLightingConfig(ambient: LightingSide?, keys: LightingSide?) {
        _state.value = _state.value.copy(
            ambient = ambient ?: _state.value.ambient,
            keys = keys ?: _state.value.keys,
        )
    }

    private fun characteristicValue(characteristic: BluetoothGattCharacteristic): ByteArray = when (characteristic) {
        inputReport -> characteristic.value ?: ByteArray(CodexProtocol.reportBodySize)
        outputReport -> characteristic.value ?: ByteArray(CodexProtocol.reportBodySize)
        batteryLevel -> byteArrayOf(_state.value.battery.toByte())
        else -> characteristic.value ?: byteArrayOf()
    }

    private fun sendReadResponse(device: BluetoothDevice, requestId: Int, offset: Int, value: ByteArray) {
        if (offset > value.size) {
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
            return
        }
        gattServer?.sendResponse(
            device,
            requestId,
            BluetoothGatt.GATT_SUCCESS,
            offset,
            value.copyOfRange(offset, value.size),
        )
    }

    private fun reportReferenceDescriptor(type: Int) = BluetoothGattDescriptor(
        reportReferenceUuid,
        BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED,
    ).apply { value = byteArrayOf(CodexProtocol.reportId.toByte(), type.toByte()) }

    private fun readCharacteristic(uuid: UUID, value: ByteArray) = BluetoothGattCharacteristic(
        uuid,
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ,
    ).apply { this.value = value }

    private fun stopTransport() {
        handler.removeCallbacksAndMessages(null)
        stopAdvertising()
        connectedDevice?.let { gattServer?.cancelConnection(it) }
        connectedDevice = null
        gattServer?.clearServices()
        gattServer?.close()
        gattServer = null
        inputReport = null
        outputReport = null
        batteryLevel = null
        pendingReports.clear()
        pendingServices.clear()
        decoder.reset()
        sendingReports = false
        inputNotificationsEnabled = false
        batteryNotificationsEnabled = false
    }

    private fun stopAdvertising() {
        if (!advertising) return
        adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        advertising = false
    }

    private fun setPhase(
        phase: ControllerPhase,
        hostName: String? = _state.value.hostName,
        message: String? = null,
    ) {
        _state.value = _state.value.copy(phase = phase, hostName = hostName, message = message)
        updateNotification()
    }

    private fun fail(message: String) {
        controllerStarted = false
        preferences.edit { putBoolean(controllerRunningKey, false) }
        stopTransport()
        if (!_settings.value.stableConnection) restoreBluetoothName()
        setPhase(ControllerPhase.ERROR, hostName = null, message = message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundActive = false
        stopSelf()
    }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    }

    private fun saveAndSetBluetoothName(bluetoothAdapter: BluetoothAdapter): Boolean {
        val currentName = bluetoothAdapter.name ?: "Android"
        if (currentName == deviceName) return true
        preferences.edit(commit = true) {
            putString(originalNameKey, currentName)
            putBoolean(renameActiveKey, true)
        }
        return bluetoothAdapter.setName(deviceName)
    }

    private fun loadSettings(): ControllerSettings {
        val storedLayers = preferences.getString(layerKeycapsKey, null)
        val layers = if (storedLayers != null) {
            CommandKeycap.decodeLayers(storedLayers)
        } else {
            CommandKeycap.defaultLayers.toMutableList().apply {
                this[0] = CommandKeycap.decodeLayout(preferences.getString(commandKeycapsKey, null))
            }
        }
        return ControllerSettings(
            stableConnection = preferences.getBoolean(stableConnectionKey, false),
            autoResume = preferences.getBoolean(autoResumeKey, false),
            activeLayer = preferences.getInt(activeLayerKey, 0).coerceIn(0, CommandKeycap.layerCount - 1),
            layerKeycaps = layers,
        )
    }

    private fun recoverBluetoothName(): Boolean {
        if (!hasBluetoothPermissions()) return false
        if (!preferences.getBoolean(renameActiveKey, false)) return false
        val originalName = preferences.getString(originalNameKey, null) ?: return false
        val restored = adapter?.takeIf { it.isEnabled && it.name == deviceName }?.setName(originalName) == true
        clearSavedBluetoothName()
        return restored
    }

    private fun restoreBluetoothName() {
        if (!preferences.getBoolean(renameActiveKey, false)) {
            clearSavedBluetoothName()
            return
        }
        if (!hasBluetoothPermissions()) return
        val originalName = preferences.getString(originalNameKey, null)
        if (originalName != null) adapter?.takeIf { it.isEnabled && it.name == deviceName }?.setName(originalName)
        clearSavedBluetoothName()
    }

    private fun clearSavedBluetoothName() {
        preferences.edit {
            remove(originalNameKey)
            remove(renameActiveKey)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            notificationChannelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val controllerAction = if (controllerStarted) actionStop else actionStart
        val controllerActionIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CodexMicroService::class.java).setAction(controllerAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when (_state.value.phase) {
            ControllerPhase.CONNECTED -> "Connected to ${_state.value.hostName ?: "macOS"}"
            ControllerPhase.ADVERTISING -> "Waiting for macOS connection"
            ControllerPhase.STOPPED -> "Controller paused; stable connection remains active"
            else -> getString(R.string.notification_running)
        }
        return NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Codex Micro")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                getString(if (controllerStarted) R.string.notification_stop else R.string.notification_start),
                controllerActionIntent,
            )
            .build()
    }

    private fun updateNotification() {
        if (!foregroundActive) return
        getSystemService(NotificationManager::class.java).notify(notificationId, buildNotification())
    }

    inner class LocalBinder : Binder() {
        fun service(): CodexMicroService = this@CodexMicroService
    }

    companion object {
        const val actionStart = "com.patchself.codexmacro.action.START"
        const val actionStop = "com.patchself.codexmacro.action.STOP"

        private const val logTag = "CodexMicroService"
        private const val deviceName = "Codex Micro"
        private const val notificationChannelId = "codex_micro_controller"
        private const val notificationId = 6
        private const val preferencesName = "codex_micro_service"
        private const val originalNameKey = "original_bluetooth_name"
        private const val renameActiveKey = "rename_active"
        private const val stableConnectionKey = "stable_connection"
        private const val autoResumeKey = "auto_resume"
        private const val commandKeycapsKey = "command_keycaps"
        private const val activeLayerKey = "active_layer"
        private const val layerKeycapsKey = "layer_keycaps"
        private const val controllerRunningKey = "controller_running"
        private const val reportDelayMs = 4L
        private const val nameChangeDelayMs = 500L

        private val deviceInformationServiceUuid = uuid(0x180A)
        private val manufacturerNameUuid = uuid(0x2A29)
        private val pnpIdUuid = uuid(0x2A50)
        private val hidServiceUuid = uuid(0x1812)
        private val hidInformationUuid = uuid(0x2A4A)
        private val reportMapUuid = uuid(0x2A4B)
        private val hidControlPointUuid = uuid(0x2A4C)
        private val reportUuid = uuid(0x2A4D)
        private val protocolModeUuid = uuid(0x2A4E)
        private val batteryServiceUuid = uuid(0x180F)
        private val batteryLevelUuid = uuid(0x2A19)
        private val clientConfigUuid = uuid(0x2902)
        private val characteristicPresentationFormatUuid = uuid(0x2904)
        private val reportReferenceUuid = uuid(0x2908)

        private fun uuid(value: Int): UUID = UUID.fromString(
            "0000${value.toString(16).padStart(4, '0')}-0000-1000-8000-00805f9b34fb",
        )

        internal fun shouldAutoResume(context: Context): Boolean {
            val preferences = context.getSharedPreferences(preferencesName, MODE_PRIVATE)
            return preferences.getBoolean(autoResumeKey, false) &&
                preferences.getBoolean(controllerRunningKey, false)
        }
    }
}
