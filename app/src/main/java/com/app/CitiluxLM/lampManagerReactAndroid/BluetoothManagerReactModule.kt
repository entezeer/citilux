package com.app.CitiluxLM.lampManagerReactAndroid

import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import com.actions.ibluz.factory.BluzDeviceFactory
import com.actions.ibluz.factory.IBluzDevice
import com.actions.ibluz.factory.IBluzDevice.OnConnectionListener
import com.actions.ibluz.factory.IBluzDevice.OnDiscoveryListener
import com.app.CitiluxLM.data.BluetoothConnectionStatus
import com.app.CitiluxLM.data.BluetoothResult

class BluetoothManagerReactModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {


    private var connector: IBluzDevice? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    val foundDevices = hashMapOf<String, BluetoothResult>()
    val connectedDevices = hashMapOf<String, BluetoothResult>()

    init {
        mainHandler.post {
            connector = BluzDeviceFactory.getDevice(reactContext).apply {
                setAutoConnectDataChanel(true)
                setConnectDataChanelBackgroundSupport(true)
            }
        }
    }

    override fun getName(): String {
        return "BluetoothManagerAndroid"
    }

    // listener для просмотра состояния подключения
    @ReactMethod
    fun setupConnectionCallbacks(promise: Promise) {
        connector?.setOnConnectionListener(object : OnConnectionListener {
            override fun onConnected(p0: BluetoothDevice?) {
                p0?.let { bluetoothDevice ->
                    connectedDevices[bluetoothDevice.name ?: bluetoothDevice.toString()] =
                        BluetoothResult(bluetoothDevice, BluetoothConnectionStatus.DATA_CONNECTED)
                    promise.resolve(
                        bluetoothDevice.name ?: bluetoothDevice.toString()
                    )
                }
            }

            override fun onDisconnected(p0: BluetoothDevice?) {
                p0?.let { bluetoothDevice ->
                    connectedDevices[bluetoothDevice.name ?: bluetoothDevice.toString()] =
                        BluetoothResult(bluetoothDevice, BluetoothConnectionStatus.DISCONNECTED)
                    promise.resolve(
                        bluetoothDevice.name ?: bluetoothDevice.toString()
                    )
                }
            }
        })

    }

    // listener для просмотра состояния сканирования
    @ReactMethod
    fun setupDiscoverCallbacks(promise: Promise) {
        connector?.setOnDiscoveryListener(object : OnDiscoveryListener {

            override fun onConnectionStateChanged(p0: BluetoothDevice?, p1: Int) {
                p0?.let { bluetoothDevice ->
                    val state = BluetoothConnectionStatus.fromBluzState(p1)
                    connectedDevices[bluetoothDevice.name ?: bluetoothDevice.toString()] =
                        BluetoothResult(bluetoothDevice, state)

                    promise.resolve(
                        bluetoothDevice.name ?: bluetoothDevice.toString()
                    )
                }
            }

            override fun onDiscoveryStarted() {
                promise.resolve("Started scanning")
            }

            override fun onDiscoveryFinished() {
                promise.resolve("Finished scanning")
            }

            override fun onFound(p0: BluetoothDevice?) {
                p0?.let { bluetoothDevice ->
                    foundDevices[bluetoothDevice.name ?: bluetoothDevice.toString()] =
                        BluetoothResult(bluetoothDevice, BluetoothConnectionStatus.DISCONNECTED)
                    promise.resolve(
                        bluetoothDevice.name ?: bluetoothDevice.toString()
                    )
                }
            }
        })

    }

    // Start device discovery
    @ReactMethod()
    fun scan() {
        connector?.startDiscovery()
    }

    // Connect a device
    @ReactMethod()
    fun connectDevice(deviceName: String) {
        connector?.connect(foundDevices[deviceName]?.device)
    }

    // Disconnect a device
    @ReactMethod()
    fun disconnectDevice(deviceName: String) {
        connector?.disconnect(connectedDevices[deviceName]?.device)
    }
}