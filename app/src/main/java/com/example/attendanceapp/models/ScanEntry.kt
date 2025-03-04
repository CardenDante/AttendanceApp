package com.example.attendanceapp.models

data class ScanEntry(
    val timestamp: String,
    val id: String,
    val name: String,
    val status: String,
    val message: String
)