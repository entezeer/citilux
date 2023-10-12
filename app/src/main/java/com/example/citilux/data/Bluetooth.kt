package com.example.citilux.data

import android.bluetooth.BluetoothDevice
import com.actions.ibluz.factory.BluzDeviceFactory

enum class BluetoothConnectionStatus {
    CONNECTING, MEDIA_CONNECTED, DATA_CONNECTED, DISCONNECTED, ERROR, OTHER;

    companion object {
        fun fromBluzState(stateCode: Int) = when (stateCode) {
            BluzDeviceFactory.ConnectionState.A2DP_CONNECTING,
            BluzDeviceFactory.ConnectionState.SPP_CONNECTING,
            BluzDeviceFactory.ConnectionState.A2DP_PAIRING -> CONNECTING

            BluzDeviceFactory.ConnectionState.A2DP_CONNECTED -> MEDIA_CONNECTED
            BluzDeviceFactory.ConnectionState.SPP_CONNECTED -> DATA_CONNECTED

            BluzDeviceFactory.ConnectionState.A2DP_DISCONNECTED,
            BluzDeviceFactory.ConnectionState.SPP_DISCONNECTED -> DISCONNECTED

            BluzDeviceFactory.ConnectionState.A2DP_FAILURE,
            BluzDeviceFactory.ConnectionState.SPP_FAILURE -> ERROR

            else -> OTHER
        }
    }
}

data class BluetoothResult(
    val device: BluetoothDevice,
    val state: BluetoothConnectionStatus,
    val isInitial: Boolean = false
) {
    val isConnected = state == BluetoothConnectionStatus.DATA_CONNECTED

    override fun equals(other: Any?): Boolean {
        return (other as? BluetoothResult)?.device?.let {
            it.address == device.address
        } ?: false
    }
}