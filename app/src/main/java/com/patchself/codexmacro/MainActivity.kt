package com.patchself.codexmacro

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.patchself.codexmacro.bluetooth.CodexMicroService
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.ui.CodexMicroApp
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

class MainActivity : ComponentActivity() {
    private val boundService = mutableStateOf<CodexMicroService?>(null)
    private var isBound = false
    private var pendingStart = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            boundService.value = (binder as? CodexMicroService.LocalBinder)?.service()
            isBound = boundService.value != null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService.value = null
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (pendingStart) ensureReadyAndStart()
    }

    private val bluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (pendingStart && result.resultCode == Activity.RESULT_OK) ensureReadyAndStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            CodexMacroTheme {
                val service = boundService.value
                val state = if (service != null) {
                    val currentState by service.state.collectAsState()
                    currentState
                } else {
                    ControllerState()
                }
                val settings = if (service != null) {
                    val currentSettings by service.settings.collectAsState()
                    currentSettings
                } else {
                    ControllerSettings()
                }
                CodexMicroApp(
                    state = state,
                    settings = settings,
                    onSettingsChange = { service?.updateSettings(it) },
                    onStart = {
                        pendingStart = true
                        ensureReadyAndStart()
                    },
                    onStop = { service?.stopController() },
                    onOpenBluetoothSettings = {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    onKey = { key, action, agent -> service?.sendKey(key, action, agent) },
                    onJoystick = { angle, distance -> service?.sendJoystick(angle, distance) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindControllerService()
    }

    override fun onStop() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            boundService.value = null
        }
        super.onStop()
    }

    private fun ensureReadyAndStart() {
        val missingPermissions = requiredBluetoothPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            val requested = missingPermissions.toMutableList()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requested += Manifest.permission.POST_NOTIFICATIONS
            }
            permissionLauncher.launch(requested.toTypedArray())
            return
        }

        val adapter = getSystemService(android.bluetooth.BluetoothManager::class.java).adapter
        if (!adapter.isEnabled) {
            bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        pendingStart = false
        ContextCompat.startForegroundService(
            this,
            Intent(this, CodexMicroService::class.java).setAction(CodexMicroService.actionStart),
        )
        bindControllerService()
    }

    private fun requiredBluetoothPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            emptyList()
        }

    private fun bindControllerService() {
        if (isBound) return
        isBound = bindService(
            Intent(this, CodexMicroService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
    }
}
