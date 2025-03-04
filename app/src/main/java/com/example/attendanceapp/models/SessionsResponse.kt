package com.example.attendanceapp.models

data class SessionsResponse(
    val success: Boolean,
    val day: String,
    val sessions: List<Session>
)