package com.lucasmarujo.gitwallpaper.data.auth

enum class DeviceFlowError {
    EXPIRED,
    ACCESS_DENIED,
    DEVICE_FLOW_DISABLED,
    INVALID_CLIENT,
    UNKNOWN,
}

class DeviceFlowException(val error: DeviceFlowError) : Exception("GitHub device flow failed: $error")
