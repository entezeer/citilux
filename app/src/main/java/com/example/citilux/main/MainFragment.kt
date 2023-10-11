package com.example.citilux.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.btw.citilux.device.feature.bluetooth.BluetoothManager
import com.example.citilux.databinding.FragmentMainBinding
import com.example.citilux.main.adapter.BluetoothDevicesAdapter
import com.example.citilux.utils.BluetoothResult

class MainFragment : Fragment(), BluetoothDevicesAdapter.Listener {

    private var binding: FragmentMainBinding? = null
    private val bluetoothManager: BluetoothManager by lazy { BluetoothManager(requireContext()) }
    private val adapter by lazy { BluetoothDevicesAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(layoutInflater)

        setupViews()
        subscribeToLiveData()

        return binding?.root
    }

    private fun setupViews() {
        with(binding) {
            this?.btnSearch?.setOnClickListener {
                startScan()
                this.progressView.visibility = View.VISIBLE
            }
            this?.devicesList?.adapter = adapter
        }
    }

    private fun subscribeToLiveData() {
        bluetoothManager.foundDevicesLiveData.observe(viewLifecycleOwner) {
            it?.let {
                adapter.addItems(it.toList())
                binding?.progressView?.visibility = View.GONE
            }
        }
    }

    private fun startScan() {
        bluetoothManager.scan()
    }

    override fun onClick(item: BluetoothResult) {
        if (!item.isConnected)
            bluetoothManager.connectDevice(item)
    }

    override fun onLongClick(item: BluetoothResult) {
        if (item.isConnected)
            bluetoothManager.disconnectDevice(item)
    }
}