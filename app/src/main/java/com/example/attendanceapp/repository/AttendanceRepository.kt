package com.example.attendanceapp.repository

import com.example.attendanceapp.api.ApiClient
import com.example.attendanceapp.models.*

class AttendanceRepository {
    private val apiService = ApiClient.apiService

    suspend fun getScannerData(testDay: String? = null): ScannerDataResponse {
        try {
            val response = apiService.getScannerData(testDay)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                return data
            } else {
                throw Exception("Failed to load scanner data: ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun verifyAttendance(uniqueId: String, sessionTime: String): VerificationResponse {
        val request = VerificationRequest(uniqueId, sessionTime)
        val response = apiService.verifyAttendance(request)
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Failed to verify attendance: ${response.message()}")
        }
    }

    suspend fun getAvailableSessions(day: String? = null): SessionsResponse {
        val response = apiService.getAvailableSessions(day)
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Failed to load sessions: ${response.message()}")
        }
    }

    suspend fun clearHistory() {
        val response = apiService.clearHistory()
        if (!response.isSuccessful) {
            throw Exception("Failed to clear history: ${response.message()}")
        }
    }
}