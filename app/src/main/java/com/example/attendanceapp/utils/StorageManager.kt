package com.example.attendanceapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.attendanceapp.models.ScanEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("attendance_app", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Save scan history
    fun saveRecentScans(scans: List<ScanEntry>) {
        val json = gson.toJson(scans)
        prefs.edit().putString("recent_scans", json).apply()
    }

    // Get scan history
    fun getRecentScans(): List<ScanEntry> {
        val json = prefs.getString("recent_scans", null) ?: return emptyList()
        val type = object : TypeToken<List<ScanEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    // Add a new scan to history
    fun addScan(scan: ScanEntry) {
        val currentScans = getRecentScans().toMutableList()
        currentScans.add(0, scan) // Add at beginning
        saveRecentScans(currentScans.take(10)) // Keep only the 10 most recent
    }

    // Add multiple scans (optional)
    fun addAllScans(scans: List<ScanEntry>) {
        val currentScans = getRecentScans().toMutableList()
        currentScans.addAll(0, scans) // Add at beginning
        saveRecentScans(currentScans.take(10)) // Keep only the 10 most recent
    }

    // Clear history
    fun clearScanHistory() {
        prefs.edit().remove("recent_scans").apply()
    }
}