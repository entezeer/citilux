package com.example.citilux.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.btw.citilux.device.feature.bluetooth.BluetoothManager
import com.example.citilux.ui.device.DeviceFragment
import com.example.citilux.databinding.FragmentMainBinding
import com.example.citilux.ui.main.adapter.BluetoothDevicesAdapter
import com.example.citilux.data.BluetoothResult

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
        if (item.isConnected) showDeviceBottomSheet()
        else bluetoothManager.connectDevice(item)
    }

    override fun onLongClick(item: BluetoothResult) {
        if (item.isConnected)
            bluetoothManager.disconnectDevice(item)
    }

    private fun showDeviceBottomSheet() {
        DeviceFragment(bluetoothManager).show(childFragmentManager, "")
    }
}