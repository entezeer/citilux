package com.app.CitiluxLM.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.CitiluxLM.R
import com.app.CitiluxLM.databinding.ItemBluetoothDevicesBinding
import com.app.CitiluxLM.data.BluetoothConnectionStatus
import com.app.CitiluxLM.data.BluetoothResult


class BluetoothDevicesAdapter(var listener: Listener) :
    RecyclerView.Adapter<BluetoothDevicesAdapter.ViewHolder>() {

    private val items: ArrayList<BluetoothResult> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBluetoothDevicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bindData(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }


    inner class ViewHolder(private val binding: ItemBluetoothDevicesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: BluetoothResult) {
            itemView.setOnClickListener {
                listener.onClick(item)
            }
            itemView.setOnLongClickListener {
                listener.onLongClick(item)
                true
            }
            binding.bluetoothDeviceLabelView.text = item.device.name ?: item.device.toString()
            binding.bluetoothDeviceIconView.setImageResource(
                if (item.isConnected)
                    R.drawable.ic_bluetooth_connected
                else R.drawable.ic_bluetooth
            )

            binding.bluetoothDeviceStatusView.setText(
                when (item.state) {
                    BluetoothConnectionStatus.CONNECTING -> R.string.device_connecting
                    BluetoothConnectionStatus.MEDIA_CONNECTED -> R.string.device_media_connected
                    BluetoothConnectionStatus.DATA_CONNECTED -> R.string.device_connected
                    else -> R.string.device_disconnected
                }
            )
        }
    }

    fun addItems(list: List<BluetoothResult>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    interface Listener {
        fun onClick(item: BluetoothResult)
        fun onLongClick(item: BluetoothResult)
    }
}