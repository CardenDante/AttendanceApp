package com.example.attendanceapp.models

data class VerificationResponse(
    val success: Boolean,
    val message: String,
    val participant: Participant?
)