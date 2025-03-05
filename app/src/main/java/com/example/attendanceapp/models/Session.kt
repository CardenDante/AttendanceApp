package com.example.attendanceapp.models

data class Session(
    val id: Int,
    val time_slot: String,
    val display_text: String? = null // This might be critical
)