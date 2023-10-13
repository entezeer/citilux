package com.app.CitiluxLM.custom_modules

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import com.actions.ibluz.factory.BluzDeviceFactory
import com.actions.ibluz.factory.IBluzDevice
import com.actions.ibluz.factory.IBluzDevice.OnConnectionListener
import com.actions.ibluz.factory.IBluzDevice.OnDiscoveryListener
import com.app.CitiluxLM.data.BluetoothConnectionStatus
import com.app.CitiluxLM.data.BluetoothResult

class BluetoothManagerReactModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var connector: IBluzDevice? = BluzDeviceFactory.getDevice(reactContext).apply {
        setAutoConnectDataChanel(true)
        setConnectDataChanelBackgroundSupport(true)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun getName(): String {
        return "BluetoothManagerAndroid"
    }

    // listener для просмотра состояния подключения
    @ReactMethod
    fun setupConnectionCallbacks(promise: Promise) {
        mainHandler.post {
            connector?.setOnConnectionListener(object : OnConnectionListener{
                override fun onConnected(p0: BluetoothDevice?) {
                    p0?.let { bluetoothDevice ->
                        promise.resolve(BluetoothResult(bluetoothDevice, BluetoothConnectionStatus.DATA_CONNECTED))
                    }
                }

                override fun onDisconnected(p0: BluetoothDevice?) {
                    p0?.let { bluetoothDevice ->
                        promise.resolve(BluetoothResult(bluetoothDevice, BluetoothConnectionStatus.DISCONNECTED))
                    }
                }
            })
        }
    }

    // listener для просмотра состояния сканирования
    @ReactMethod
    fun setupDiscoverCallbacks(promise: Promise) {
        mainHandler.post {
            connector?.setOnDiscoveryListener(object : OnDiscoveryListener {

                override fun onConnectionStateChanged(p0: BluetoothDevice?, p1: Int) {
                    p0?.let { bluetoothDevice ->
                        val state = BluetoothConnectionStatus.fromBluzState(p1)
                        promise.resolve(BluetoothResult(bluetoothDevice, state))
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
                        promise.resolve(
                            BluetoothResult(
                                bluetoothDevice,
                                BluetoothConnectionStatus.DISCONNECTED
                            )
                        )
                    }
                }
            })
        }
    }

    // Start device discovery
    @ReactMethod()
    fun scan() {
        connector?.startDiscovery()
    }

    // Connect a device
    @ReactMethod()
    fun connectDevice(device: BluetoothDevice) {
        connector?.connect(device)
    }

    // Disconnect a device
    @ReactMethod()
    fun disconnectDevice(device: BluetoothDevice) {
        connector?.disconnect(device)
    }
}