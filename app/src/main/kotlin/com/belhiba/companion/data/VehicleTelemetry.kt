package com.belhiba.companion.data

import java.util.UUID

object VehicleBleConstants {
    val VEHICLE_SERVICE_UUID: UUID = UUID.fromString("0000FE20-0000-1000-8000-00805F9B34FB")
    val TELEMETRY_CHAR_UUID: UUID = UUID.fromString("0000FE21-0000-1000-8000-00805F9B34FB")
    val CONTROL_CHAR_UUID: UUID = UUID.fromString("0000FE22-0000-1000-8000-00805F9B34FB")
    val DRIVER_AUTH_CHAR_UUID: UUID = UUID.fromString("0000FE23-0000-1000-8000-00805F9B34FB")
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    const val PROXIMITY_WELCOME_RSSI_THRESHOLD = -65
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val deviceAddress: String) : ConnectionState()
    data class Connected(val vehicleVin: String, val rssi: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class VehicleTelemetry(
    val vin: String = "VF1-ELEC-2026",
    val batteryLevelPercent: Int = 84,
    val estimatedRangeKm: Int = 345,
    val isCharging: Boolean = false,
    val isLocked: Boolean = true,
    val cabinTemperatureCelsius: Float = 20.5f,
    val targetHvacTempCelsius: Float = 21.0f,
    val isHvacActive: Boolean = false,
    val tirePressureFrontLeft: Float = 2.4f,
    val tirePressureFrontRight: Float = 2.4f,
    val tirePressureRearLeft: Float = 2.3f,
    val tirePressureRearRight: Float = 2.3f,
    val odometerKm: Long = 18450L,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

sealed class VehicleCommand(val opcode: Byte) {
    object LockDoors : VehicleCommand(0x01)
    object UnlockDoors : VehicleCommand(0x02)
    data class SetCabinTemperature(val targetTemp: Float) : VehicleCommand(0x03)
    data class ToggleHvac(val enable: Boolean) : VehicleCommand(0x04)
    object FlashLightsAndHorn : VehicleCommand(0x05)
    data class AuthenticateDriver(val driverToken: ByteArray) : VehicleCommand(0x06)
}
