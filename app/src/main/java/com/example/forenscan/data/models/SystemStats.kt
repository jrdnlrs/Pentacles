package com.example.forenscan.data.models

data class SystemStats(
    val networksScanned: Int = 0,
    val threatsDetected: Int = 0,
    val lastScanTime: Long = 0,
    val totalScans: Int = 0
)