package com.app.CitiluxLM.custom_modules

import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import android.os.Handler
import android.os.Looper
import com.actions.ibluz.factory.BluzDeviceFactory
import com.actions.ibluz.factory.IBluzDevice
import com.actions.ibluz.manager.BluzManager
import com.actions.ibluz.manager.BluzManagerData
import com.app.CitiluxLM.data.Lamp
import com.app.CitiluxLM.data.LightParameters

class LampManagerReactModule(val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var connector: IBluzDevice? = BluzDeviceFactory.getDevice(reactContext).apply {
        setAutoConnectDataChanel(true)
        setConnectDataChanelBackgroundSupport(true)
    }
    var manager: BluzManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun getName(): String {
        return "LampManagerAndroid"
    }

    @ReactMethod()
    fun initManager(volumePromise: Promise, commandPromise: Promise) {
        mainHandler.post {
            manager = BluzManager(reactContext, connector) {
                requireNotNull(manager).run {
                    setSystemTime()
                    setForeground(true)
                    setOnGlobalUIChangedListener(getBluetoothEventListener(volumePromise))
                    setOnCustomCommandListener(getCustomCommandListener(commandPromise))
                    sendQueueCommand(135, 0, 0, byteArrayOf())
                }
            }
        }
    }

    @ReactMethod
    fun turnOnLamp(promise: Promise) {
        try {
            setLight(promise, LightParameters.ON)
            promise.resolve("Lamp turned on.")
        } catch (e: Exception) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", e.message)
        }
    }

    @ReactMethod
    fun turnOffLamp(promise: Promise) {
        try {
            sendLampCommand(0, Lamp.Mode.NORMAL)
            promise.resolve("Lamp turned off.")
        } catch (e: Exception) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", e.message)
        }
    }

    @ReactMethod
    fun setLight(promise: Promise, parameters: LightParameters) {
        try {
            promise.resolve("Lamp setLight.")
            sendLampCommand(parameters.rbgHex, parameters.lightHex)
        } catch (e: Exception) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", e.message)
        }
    }

    @ReactMethod
    fun setVolume(promise: Promise, volume: Int) {
        try {
            promise.resolve("Lamp setVolume.")
            manager?.setVolume(volume)
        } catch (e: Exception) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", e.message)
        }
    }

    private fun sendQueueCommand(keyCode: Int, arg1: Int, arg2: Int, data: ByteArray) {
        val key = BluzManager.buildKey(BluzManagerData.CommandType.QUE, keyCode)
        manager?.sendCustomCommand(key, arg1, arg2, data)
    }

    private fun getBluetoothEventListener(promise: Promise) = object : BluzManagerData.OnGlobalUIChangedListener {
        override fun onBatteryChanged(arg0: Int, arg1: Boolean) {}

        override fun onEQChanged(arg0: Int) {}

        override fun onModeChanged(arg0: Int) {}

        override fun onVolumeChanged(arg0: Int, arg1: Boolean) {
            promise.resolve(arg0)
        }
    }

    private fun getCustomCommandListener(promise: Promise) =
        BluzManagerData.OnCustomCommandListener { arg0, arg1, arg2, _ ->
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
                promise.resolve(params)
            }
        }

    private fun sendLampCommand(rbgColorRegister: Int, whiteYellowColorTypeRegister: Int) {
        val key = BluzManager.buildKey(
            BluzManagerData.CommandType.SET,
            131
        )
        manager?.sendCustomCommand(
            key,
            rbgColorRegister,
            whiteYellowColorTypeRegister, byteArrayOf()
        )
    }
}
