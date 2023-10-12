package com.example.citilux.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.citilux.R
import com.example.citilux.databinding.ActivityMainBinding
import com.example.citilux.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    private val permissions = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            android.Manifest.permission.ACCESS_FINE_LOCATION
        else android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private fun addFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, MainFragment())
            .commit()
    }


    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                addFragment()
            else
                ActivityCompat.requestPermissions(this, permissions, 100)

        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.getValue(android.Manifest.permission.BLUETOOTH_CONNECT)
                || permissions.getValue(android.Manifest.permission.BLUETOOTH_SCAN)
            ) addFragment()
            else ActivityCompat.requestPermissions(this, this.permissions, 100)
        }
}