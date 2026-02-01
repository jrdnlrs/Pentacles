package com.example.forenscan.data.models.viewmodel

import com.example.forenscan.data.models.ActivityType

// This blueprint defines what a "Timeline Event" looks like
data class HistoryItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: ActivityType // This matches your ActivityType.THREAT_DETECTED check
)