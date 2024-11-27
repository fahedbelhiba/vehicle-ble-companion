package com.belhiba.companion.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.belhiba.companion.data.VehicleBleConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DiscoveredVehicle(
    val name: String,
    val address: String,
    val rssi: Int,
    val isNearbyWelcomeZone: Boolean
)

class BleVehicleScanner(private val bluetoothAdapter: BluetoothAdapter?) {

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    fun scanForVehicles(): Flow<DiscoveredVehicle> = callbackFlow {
        val currentScanner = scanner
        if (currentScanner == null || !isBluetoothEnabled) {
            close(IllegalStateException("Bluetooth is disabled or BLE Scanner is unavailable"))
            return@callbackFlow
        }

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(VehicleBleConstants.VEHICLE_SERVICE_UUID))
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { res ->
                    val device = res.device
                    val name = device.name ?: "Connected Vehicle"
                    val isNearby = res.rssi >= VehicleBleConstants.PROXIMITY_WELCOME_RSSI_THRESHOLD

                    trySend(DiscoveredVehicle(
                        name = name,
                        address = device.address,
                        rssi = res.rssi,
                        isNearbyWelcomeZone = isNearby
                    ))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(RuntimeException("BLE Scan failed with error code: $errorCode"))
            }
        }

        currentScanner.startScan(filters, settings, callback)
        awaitClose { currentScanner.stopScan(callback) }
    }
}
