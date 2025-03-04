package com.example.attendanceapp.models

data class ScannerDataResponse(
    val success: Boolean,
    val day_name: String,
    val current_time: String,
    val sessions: List<Session>,
    val current_session: Session?,
    val recent_scans: List<ScanEntry>
)