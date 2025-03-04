// api/ApiService.kt
package com.example.attendanceapp.api

import com.example.attendanceapp.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("/api/scanner_data")
    suspend fun getScannerData(
        @Query("test_day") testDay: String? = null
    ): Response<ScannerDataResponse>

    @POST("/api/verify")
    suspend fun verifyAttendance(
        @Body request: VerificationRequest
    ): Response<VerificationResponse>

    @GET("/api/sessions")
    suspend fun getAvailableSessions(
        @Query("day") day: String? = null
    ): Response<SessionsResponse>

    @GET("/api/clear-history")
    suspend fun clearHistory(): Response<Map<String, Any>>
}