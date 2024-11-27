package com.belhiba.companion.repository

import com.belhiba.companion.ble.BleVehicleScanner
import com.belhiba.companion.ble.DiscoveredVehicle
import com.belhiba.companion.ble.VehicleGattClient
import com.belhiba.companion.data.ConnectionState
import com.belhiba.companion.data.VehicleCommand
import com.belhiba.companion.data.VehicleTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    val connectionState: StateFlow<ConnectionState>
    val telemetry: StateFlow<VehicleTelemetry>
    fun scanForVehicles(): Flow<DiscoveredVehicle>
    fun connectToVehicle(address: String)
    fun disconnectVehicle()
    suspend fun lockDoors(): Boolean
    suspend fun unlockDoors(): Boolean
    suspend fun toggleHvac(enable: Boolean): Boolean
    suspend fun setTargetTemperature(celsius: Float): Boolean
    suspend fun flashLightsAndHorn(): Boolean
}

class VehicleRepositoryImpl(
    private val scanner: BleVehicleScanner,
    private val gattClient: VehicleGattClient
) : VehicleRepository {
    override val connectionState: StateFlow<ConnectionState> = gattClient.connectionState
    override val telemetry: StateFlow<VehicleTelemetry> = gattClient.telemetry

    override fun scanForVehicles(): Flow<DiscoveredVehicle> = scanner.scanForVehicles()
    override fun connectToVehicle(address: String) = gattClient.connect(address)
    override fun disconnectVehicle() = gattClient.disconnect()

    override suspend fun lockDoors(): Boolean = gattClient.sendCommand(VehicleCommand.LockDoors)
    override suspend fun unlockDoors(): Boolean = gattClient.sendCommand(VehicleCommand.UnlockDoors)
    override suspend fun toggleHvac(enable: Boolean): Boolean = gattClient.sendCommand(VehicleCommand.ToggleHvac(enable))
    override suspend fun setTargetTemperature(celsius: Float): Boolean = gattClient.sendCommand(VehicleCommand.SetCabinTemperature(celsius))
    override suspend fun flashLightsAndHorn(): Boolean = gattClient.sendCommand(VehicleCommand.FlashLightsAndHorn)
}
