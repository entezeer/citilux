package com.app.CitiluxLM.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.actions.ibluz.factory.BluzDeviceFactory
import com.actions.ibluz.factory.IBluzDevice
import com.actions.ibluz.factory.IBluzDevice.OnConnectionListener
import com.actions.ibluz.factory.IBluzDevice.OnDiscoveryListener
import com.actions.ibluz.manager.BluzManager
import com.actions.ibluz.manager.BluzManagerData.*
import com.app.CitiluxLM.data.LightParameters
import com.app.CitiluxLM.data.BluetoothConnectionStatus
import com.app.CitiluxLM.data.BluetoothResult
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private const val MAX_RETRY_COUNT = 5
private const val SCAN_TIMEOUT_MS = 5000L
private const val LAMP_NAME = "CITILUX"

@SuppressLint("NewApi")
class BluetoothManager(
    private val context: Context,
) {

    private var connector: IBluzDevice? = BluzDeviceFactory.getDevice(context).apply {
        setAutoConnectDataChanel(true)
        setConnectDataChanelBackgroundSupport(true)
    }

    private var manager: BluzManager? = null

    private val connectedDeviceCount: AtomicInteger = AtomicInteger(0)
    private val foundDeviceCount: AtomicInteger = AtomicInteger(0)
    private val scanRetryCount: AtomicInteger = AtomicInteger(0)
    private val scanDevices = Collections.synchronizedSet<BluetoothResult>(mutableSetOf())
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        setupOnConnectionListener()
        setupOnDiscoveryListener()
    }

    // LiveData for connected devices
    val connectedDevicesLiveData = MutableLiveData<BluetoothResult>()

    // LiveData for found devices
    val foundDevicesLiveData = MutableLiveData<Set<BluetoothResult>>()

    // LiveData for connection status
    val connectionStatusLiveData = MutableLiveData<BluetoothConnectionStatus>()

    // LiveData for lamp light parameters
    val lampLightLiveData = MutableLiveData<LightParameters>()

    // LiveData for lamp volume
    val lampVolumeLiveData = MutableLiveData<Int>()

    // Setup listeners
    private fun setupOnConnectionListener() {
        mainHandler.post {
            connector?.setOnConnectionListener(object : OnConnectionListener {
                override fun onConnected(p0: BluetoothDevice?) {
                    p0?.let { device ->
                        connectedDeviceCount.getAndIncrement()
                        emitConnectedDevice(device, BluetoothConnectionStatus.DATA_CONNECTED)
                    }
                }

                override fun onDisconnected(p0: BluetoothDevice?) {
                    p0?.let { device ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            connectedDeviceCount.getAndUpdate { (it - 1).coerceAtLeast(0) }
                        }
                        emitConnectedDevice(device, BluetoothConnectionStatus.DISCONNECTED)
                    }
                }
            })
        }
    }

    private fun setupOnDiscoveryListener() {
        mainHandler.postDelayed({
            connector?.setOnDiscoveryListener(object : OnDiscoveryListener {
                override fun onConnectionStateChanged(
                    p0: BluetoothDevice?,
                    p1: Int
                ) {
                    p0?.let { device ->
                        val state = BluetoothConnectionStatus.fromBluzState(p1)
                        val entry = BluetoothResult(device, state)
                        emitFoundDevice(entry)
                    }
                }

                override fun onDiscoveryStarted() {
                    scanDevices.clear()
                }

                override fun onDiscoveryFinished() {
                    if (scanRetryCount.getAndIncrement() == 0) connector?.startDiscovery()
                    connector?.connectedDevice?.let {
                        emitFoundDevice(BluetoothResult(it, BluetoothConnectionStatus.DATA_CONNECTED))
                    }
                }

                override fun onFound(p0: BluetoothDevice?) {
                    p0?.let { device ->
                        foundDeviceCount.getAndIncrement()

                        val entry = BluetoothResult(
                            device,
                            BluetoothConnectionStatus.DISCONNECTED,
                        )
                        emitFoundDevice(entry)
                    }
                }

            })
        }, SCAN_TIMEOUT_MS)
    }

    // Initialize the manager on the main thread
    private fun initManager() {
        mainHandler.post {
            manager = BluzManager(context, connector) {
                requireNotNull(manager).run {
                    setSystemTime()
                    setForeground(true)
                    setOnGlobalUIChangedListener(bluetoothEventListener)
                    setOnCustomCommandListener(customCommandListener)
                    sendQueueCommand(135, 0, 0, byteArrayOf())
                }
            }
        }
    }

    // Start device discovery
    fun scan() {
        foundDeviceCount.set(0)
        connector?.startDiscovery()
    }

    // Connect a device
    fun connectDevice(result: BluetoothResult) {
        connector?.connect(result.device)
    }

    // Disconnect a device
    fun disconnectDevice(result: BluetoothResult) {
        connector?.disconnect(result.device)
    }

    // Send a lamp command
    fun sendLampCommand(rbgColorRegister: Int, whiteYellowColorTypeRegister: Int) {
        val key = BluzManager.buildKey(
            CommandType.SET,
            131
        )
        manager?.sendCustomCommand(
            key,
            rbgColorRegister,
            whiteYellowColorTypeRegister, byteArrayOf()
        )
    }

    // Set the volume
    fun setVolume(volume: Int) {
        manager?.setVolume(volume)
    }


    private val bluetoothEventListener = object : OnGlobalUIChangedListener {
        override fun onBatteryChanged(arg0: Int, arg1: Boolean) {}

        override fun onEQChanged(arg0: Int) {}

        override fun onModeChanged(arg0: Int) {}

        override fun onVolumeChanged(arg0: Int, arg1: Boolean) {
            // Используем Handler для обновления LiveData на основном потоке
            mainHandler.post {
                emitLampVolume(arg0)
            }
        }
    }

    private val customCommandListener = OnCustomCommandListener { arg0, arg1, arg2, _ ->
        val key = arg0 and 0x4100.inv()
        "Raw lamp signal, arg0: ${arg0.toString(16)}, arg1: ${arg1.toString(16)}, key: ${
            key.toString(
                16
            )
        }"
        if (key == 135) {
            sendQueueCommand(130, 0, 0, byteArrayOf()) // Запросить состояние света
        }
        if (arg0 == 16770) { // Установлен цвет
            val params = LightParameters.fromHex(rbgaHex = arg1, lightHex = arg2)
            // Используем Handler для обновления LiveData на основном потоке
            mainHandler.post {
                emitLampLight(params)
            }
        }
    }

    private fun sendQueueCommand(keyCode: Int, arg1: Int, arg2: Int, data: ByteArray) {
        val key = BluzManager.buildKey(CommandType.QUE, keyCode)
        manager?.sendCustomCommand(key, arg1, arg2, data)
    }


    //  Use LiveData instead Flow emit for data
    private fun emitConnectedDevice(device: BluetoothDevice, status: BluetoothConnectionStatus) {
        connectedDevicesLiveData.postValue(BluetoothResult(device, status))
        emitConnectionStatus(status)
        val entry = BluetoothResult(device, status)
        scanDevices.removeIf { it == entry }
        scanDevices.add(entry)
    }


    private fun emitFoundDevice(
        entry: BluetoothResult
    ) {
        val canRetry = { d: BluetoothDevice ->
            val retries = scanRetryCount.getAndIncrement()
            if (retries <= MAX_RETRY_COUNT) {
                connector?.retry(d)
                true
            } else {
                false
            }
        }

        var newState = entry.state
        if (entry.state == BluetoothConnectionStatus.ERROR) {
            newState = BluetoothConnectionStatus.DISCONNECTED
            canRetry(entry.device)
        }
        entry.copy(state = newState)

        foundDeviceCount.getAndIncrement()
        scanDevices.removeIf { it == entry }
        scanDevices.add(entry)
        foundDevicesLiveData.value = scanDevices
    }

    private fun emitConnectionStatus(status: BluetoothConnectionStatus) {
        if (status == BluetoothConnectionStatus.DATA_CONNECTED) initManager()
        else manager?.release()
        connectionStatusLiveData.postValue(status)
    }

    private fun emitLampLight(lightParameters: LightParameters) {
        lampLightLiveData.postValue(lightParameters)
    }

    private fun emitLampVolume(volume: Int) {
        lampVolumeLiveData.postValue(volume)
    }
}
