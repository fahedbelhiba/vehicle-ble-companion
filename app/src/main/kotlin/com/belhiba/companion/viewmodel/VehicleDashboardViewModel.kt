package com.belhiba.companion.viewmodel

import com.belhiba.companion.data.ConnectionState
import com.belhiba.companion.data.VehicleTelemetry
import com.belhiba.companion.repository.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehicleDashboardViewModel(
    private val repository: VehicleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val telemetry: StateFlow<VehicleTelemetry> = repository.telemetry

    fun onLockToggleRequested() {
        scope.launch {
            val isCurrentlyLocked = telemetry.value.isLocked
            if (isCurrentlyLocked) {
                repository.unlockDoors()
            } else {
                repository.lockDoors()
            }
        }
    }

    fun onHvacToggleRequested(enable: Boolean) {
        scope.launch {
            repository.toggleHvac(enable)
        }
    }

    fun onTargetTemperatureChanged(newTemp: Float) {
        scope.launch {
            repository.setTargetTemperature(newTemp)
        }
    }

    fun onFindMyVehicleRequested() {
        scope.launch {
            repository.flashLightsAndHorn()
        }
    }
}
