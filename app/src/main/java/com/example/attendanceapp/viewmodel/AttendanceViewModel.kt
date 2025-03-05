package com.example.attendanceapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.attendanceapp.models.ScanEntry
import com.example.attendanceapp.models.ScannerDataResponse
import com.example.attendanceapp.models.Session
import com.example.attendanceapp.models.SessionsResponse
import com.example.attendanceapp.models.VerificationResponse
import com.example.attendanceapp.repository.AttendanceRepository
import com.example.attendanceapp.utils.StorageManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AttendanceRepository()
    private val storageManager = StorageManager(application)

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

    // Local scan history
    private val _localScanHistory = MutableLiveData<List<ScanEntry>>()
    val localScanHistory: LiveData<List<ScanEntry>> = _localScanHistory

    init {
        // Load saved scans when ViewModel is created
        _localScanHistory.value = storageManager.getRecentScans()
    }

    fun loadScannerData(testDay: String? = null) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = repository.getScannerData(testDay)

                // If we want to incorporate server scans with local ones
                // (Optional - you can remove this if you only want local scans)
                val serverScans = result.recent_scans
                if (serverScans.isNotEmpty()) {
                    storageManager.addAllScans(serverScans)
                    _localScanHistory.value = storageManager.getRecentScans()
                }

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

                // Add scan to local history
                val scan = ScanEntry(
                    timestamp = getCurrentTime(),
                    id = uniqueId,
                    name = result.participant?.name ?: "Unknown",
                    status = if (result.success) "Correct" else "Incorrect",
                    message = result.message ?: ""
                )
                storageManager.addScan(scan)

                // Update LiveData with new scan list
                _localScanHistory.value = storageManager.getRecentScans()

                _loading.value = false
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
                // Clear server history
                repository.clearHistory()

                // Clear local history
                storageManager.clearScanHistory()
                _localScanHistory.value = emptyList()

                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}