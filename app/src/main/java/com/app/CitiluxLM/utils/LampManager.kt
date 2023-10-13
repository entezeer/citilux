package com.app.CitiluxLM.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.app.CitiluxLM.BuildConfig
import com.app.CitiluxLM.data.BluetoothConnectionStatus
import com.app.CitiluxLM.data.Lamp
import com.app.CitiluxLM.data.LightParameters
import com.app.CitiluxLM.data.RawLight
import com.app.CitiluxLM.data.WhiteWarmthLight
import com.app.CitiluxLM.extensions.ledNormalized
import com.app.CitiluxLM.extensions.rgbNormalized

const val BROADCAST_ACTION_CONTROL_LAUNCH = "com.btw.citilux.CONTROL_LAUNCH"
const val BROADCAST_ACTION_CONTROL_EXIT = "com.btw.citilux.CONTROL_EXIT"
const val BROADCAST_ACTION_CONTROL_ON_OFF = "com.btw.citilux.CONTROL_ON_OFF"
const val BROADCAST_ACTION_CONTROL_MAX = "com.btw.citilux.CONTROL_MAX"
const val BROADCAST_ACTION_CONTROL_NIGHT = "com.btw.citilux.CONTROL_NIGHT"
const val BROADCAST_ACTION_CONTROL_M1 = "com.btw.citilux.CONTROL_M1"

class LampManager(private val bluetoothManager: BluetoothManager) {
    val isConnected: LiveData<Boolean> = Transformations.map(bluetoothManager.connectionStatusLiveData) {
        it == BluetoothConnectionStatus.DATA_CONNECTED
    }

    val lightFlow: LiveData<LightParameters> = bluetoothManager.lampLightLiveData

    val volumeFlow: LiveData<Int> = bluetoothManager.lampVolumeLiveData

    var lastLight: LightParameters? = null
        private set

    fun turnOff() {
        bluetoothManager.sendLampCommand(0, Lamp.Mode.NORMAL)
        bluetoothManager.lampLightLiveData.postValue(LightParameters.OFF)
    }

    fun setLight(parameters: LightParameters) {
        val hasWhiteLight: (LightParameters) -> Boolean = { it.white != 0 || it.yellow != 0 }
        val parametersNormalized = RawLight(
            rgb = parameters.rgb.let {
                if (!hasWhiteLight(parameters) && it == Lamp.Colors.RGB_BLACK)
                    it.rgbNormalized
                else it
            },
            white = parameters.white.let {
                if (parameters is WhiteWarmthLight || hasWhiteLight(parameters))
                    it.ledNormalized
                else it
            },
            yellow = parameters.yellow.let {
                if (parameters is WhiteWarmthLight || hasWhiteLight(parameters))
                    it.ledNormalized
                else it
            },
            mode = parameters.mode
        )
        if (BuildConfig.DEBUG) {
            // Test lamp is in RGB mode
            bluetoothManager.sendLampCommand(
                parametersNormalized.rgb,
                parametersNormalized.lightHex
            )
        } else {
            // Production lamps are in RBG mode, not RGB
            bluetoothManager.sendLampCommand(
                parametersNormalized.rbgHex,
                parametersNormalized.lightHex
            )
        }
        lastLight = lightFlow.value
        bluetoothManager.lampLightLiveData.postValue(parameters)
    }

    fun setVolume(volume: Int) {
        bluetoothManager.setVolume(volume.coerceIn(0, Lamp.LAMP_VOLUME_MAX))
    }
}