package com.example.attendanceapp.models

data class VerificationRequest(
    val unique_id: String,
    val session_time: String
)