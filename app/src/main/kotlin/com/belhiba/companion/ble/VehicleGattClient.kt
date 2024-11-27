package com.belhiba.companion.ble

import android.bluetooth.*
import android.content.Context
import com.belhiba.companion.data.ConnectionState
import com.belhiba.companion.data.VehicleBleConstants
import com.belhiba.companion.data.VehicleCommand
import com.belhiba.companion.data.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VehicleGattClient(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetry = MutableStateFlow(VehicleTelemetry())
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.Connecting(gatt.device.address)
                    gatt.requestMtu(512)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.Disconnected
                    cleanup()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(VehicleBleConstants.VEHICLE_SERVICE_UUID)
                val telemetryChar = service?.getCharacteristic(VehicleBleConstants.TELEMETRY_CHAR_UUID)
                if (telemetryChar != null) {
                    enableTelemetryNotifications(gatt, telemetryChar)
                    _connectionState.value = ConnectionState.Connected(
                        vehicleVin = gatt.device.name ?: "VF1-ELEC-2026",
                        rssi = -60
                    )
                }
            }
        }

        @Deprecated("Deprecated in Java but required for backwards compatibility API < 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == VehicleBleConstants.TELEMETRY_CHAR_UUID) {
                characteristic.value?.let { parseTelemetryPacket(it) }
            }
        }
    }

    fun connect(deviceAddress: String) {
        val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            ?: run {
                _connectionState.value = ConnectionState.Error("Invalid Bluetooth Device Address")
                return
            }
        _connectionState.value = ConnectionState.Connecting(deviceAddress)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun sendCommand(command: VehicleCommand): Boolean {
        val gatt = bluetoothGatt ?: return false
        val service = gatt.getService(VehicleBleConstants.VEHICLE_SERVICE_UUID) ?: return false
        val controlChar = service.getCharacteristic(VehicleBleConstants.CONTROL_CHAR_UUID) ?: return false

        val payload = when (command) {
            is VehicleCommand.LockDoors -> byteArrayOf(command.opcode)
            is VehicleCommand.UnlockDoors -> byteArrayOf(command.opcode)
            is VehicleCommand.ToggleHvac -> byteArrayOf(command.opcode, if (command.enable) 1 else 0)
            is VehicleCommand.SetCabinTemperature -> {
                val buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(command.opcode)
                buffer.putFloat(command.targetTemp)
                buffer.array()
            }
            is VehicleCommand.FlashLightsAndHorn -> byteArrayOf(command.opcode)
            is VehicleCommand.AuthenticateDriver -> byteArrayOf(command.opcode) + command.driverToken
        }

        controlChar.value = payload
        return gatt.writeCharacteristic(controlChar)
    }

    private fun enableTelemetryNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(VehicleBleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }

    fun parseTelemetryPacket(bytes: ByteArray) {
        if (bytes.size < 12) return
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val batteryPercent = buffer.get().toInt() and 0xFF
        val rangeKm = buffer.short.toInt() and 0xFFFF
        val flags = buffer.get().toInt()
        val isCharging = (flags and 0x01) != 0
        val isLocked = (flags and 0x02) != 0
        val isHvacActive = (flags and 0x04) != 0
        val cabinTemp = buffer.float
        val targetTemp = buffer.float

        val updated = _telemetry.value.copy(
            batteryLevelPercent = batteryPercent,
            estimatedRangeKm = rangeKm,
            isCharging = isCharging,
            isLocked = isLocked,
            cabinTemperatureCelsius = cabinTemp,
            targetHvacTempCelsius = targetTemp,
            isHvacActive = isHvacActive,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        scope.launch { _telemetry.emit(updated) }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        cleanup()
    }

    private fun cleanup() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
