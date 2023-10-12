package com.example.citilux.ui.device

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.btw.citilux.device.feature.bluetooth.BluetoothManager
import com.example.citilux.data.LightParameters
import com.example.citilux.databinding.FragmentDeviceBinding
import com.example.citilux.utils.LampManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.concurrent.atomic.AtomicBoolean

class DeviceFragment(private val bluetoothManager: BluetoothManager): BottomSheetDialogFragment() {

    private var binding: FragmentDeviceBinding? = null

    private val lampManager by lazy { LampManager(bluetoothManager) }

    private var isLampOn: AtomicBoolean = AtomicBoolean(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDeviceBinding.inflate(layoutInflater)

        setupViews()

        subscribeToLiveData()

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.rootView?.layoutParams?.height = getWindowHeight() * 50 / 100
    }

    private fun setupViews() {
        binding?.run {
            btnTurnOnOff.setOnClickListener {
                if (isLampOn.get()) {
                    lampManager.turnOff()
                    isLampOn.set(false)
                } else {
                    lampManager.setLight(LightParameters.RANDOM_COLOR)
                }
            }
        }
    }

    private fun subscribeToLiveData() {
        lampManager.lightFlow.observe(viewLifecycleOwner) {
            isLampOn.set(LightParameters.isOn(it))
        }
    }

    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }
}