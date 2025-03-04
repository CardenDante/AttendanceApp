package com.example.attendanceapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.models.ScanEntry
import com.example.attendanceapp.models.ScannerDataResponse
import com.example.attendanceapp.models.Session
import com.example.attendanceapp.models.SessionsResponse
import com.example.attendanceapp.models.VerificationResponse
import com.example.attendanceapp.repository.AttendanceRepository
import kotlinx.coroutines.launch

class AttendanceViewModel : ViewModel() {
    private val repository = AttendanceRepository()

    // Loading state
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Scanner data
    private val _scannerData = MutableLiveData<ScannerDataResponse>()
    val scannerData: LiveData<ScannerDataResponse> = _scannerData

    // Verification result
    private val _verificationResult = MutableLiveData<VerificationResponse>()
    val verificationResult: LiveData<VerificationResponse> = _verificationResult

    // Available sessions
    private val _sessions = MutableLiveData<List<Session>>()
    val sessions: LiveData<List<Session>> = _sessions

    fun loadScannerData(testDay: String? = null) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = repository.getScannerData(testDay)
                _scannerData.value = result
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    fun verifyAttendance(uniqueId: String, sessionTime: String) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = repository.verifyAttendance(uniqueId, sessionTime)
                _verificationResult.value = result
                _loading.value = false
                // Refresh scanner data to update recent scans
                loadScannerData()
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    fun loadAvailableSessions(day: String? = null) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = repository.getAvailableSessions(day)
                _sessions.value = result.sessions
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    fun clearHistory() {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                repository.clearHistory()
                // Refresh scanner data
                loadScannerData()
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}