# 🚗 Vehicle BLE Companion App (Android / Kotlin)

**Author:** Fahd BELHIBA  
**Stack:** Kotlin, Android Studio, Coroutines, StateFlow, BLE GATT API, MVVM Architecture

## Overview
A reactive Android companion app designed to interact seamlessly with in-vehicle head units via Bluetooth Low Energy (BLE). It features hands-free welcome unlock based on RSSI proximity filtering, real-time telemetry streaming (Battery SoC, Range, Cabin Temperature), and remote commands (door lock/unlock, climate pre-conditioning).

### Key Technical Highlights:
- **Clean Architecture & MVVM:** Strict separation of GATT transport, repository caching, and reactive ViewModel layer.
- **Reactive State Management:** Kotlin Coroutines `callbackFlow` and `StateFlow` for real-time telemetry updates.
- **Automotive Protocol:** Custom GATT characteristics and binary packet serialization with MTU negotiation.
