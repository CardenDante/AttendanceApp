// ui/ScannerFragment.kt
package com.example.attendanceapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.attendanceapp.R
import com.example.attendanceapp.models.ScannerDataResponse
import com.example.attendanceapp.models.Session
import com.example.attendanceapp.viewmodel.AttendanceViewModel
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

class ScannerFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var spinnerSessions: Spinner
    private lateinit var sessionAdapter: ArrayAdapter<String>
    private lateinit var currentSession: Session
    private lateinit var sessions: List<Session>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQrScanner()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is needed to scan QR codes",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[AttendanceViewModel::class.java]

        // Initialize views
        spinnerSessions = view.findViewById(R.id.spinnerSessions)

        // Set up the spinner adapter
        sessionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ArrayList<String>()
        )
        sessionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSessions.adapter = sessionAdapter

        // Set up manual entry button
        view.findViewById<Button>(R.id.btnManualEntry).setOnClickListener {
            showManualEntryDialog()
        }

        // Set flash button
        view.findViewById<ImageView>(R.id.ivFlash)?.setOnClickListener {
            // Toggle flash logic would go here if using CameraX
            Toast.makeText(requireContext(), "Flash toggle not implemented", Toast.LENGTH_SHORT).show()
        }

        // Observe ViewModel
        viewModel.scannerData.observe(viewLifecycleOwner) { data: ScannerDataResponse ->
            // Update sessions list
            sessions = data.sessions
            val sessionTimes = sessions.map { it.time_slot }
            sessionAdapter.clear()
            sessionAdapter.addAll(sessionTimes)

            // Select current session in spinner
            data.current_session?.let { currentSession ->
                this.currentSession = currentSession
                val index = sessions.indexOfFirst { it.time_slot == currentSession.time_slot }
                if (index >= 0) {
                    spinnerSessions.setSelection(index)
                }

                // Update current session text
                view.findViewById<TextView>(R.id.tvCurrentSession)?.text =
                    "Current Session: ${currentSession.time_slot}"
            }
        }

        // Check camera permission and start scanner
        checkCameraPermissionAndStartScanner()
    }

    private fun checkCameraPermissionAndStartScanner() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            startQrScanner()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startQrScanner() {
        // For now, we'll use the ZXing library's built-in scanner
        // In a more advanced implementation, you'd set up CameraX for better control

        // Initialize scanner
        IntentIntegrator.forSupportFragment(this).apply {
            captureActivity = CaptureActivity::class.java
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan a QR code")
            setCameraId(0) // Use back camera
            setBeepEnabled(true)
            setOrientationLocked(false)
            initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // Got QR code result
                handleScannedCode(result.contents)
            } else {
                // Scanning was canceled
                Toast.makeText(context, "Scan canceled", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScannedCode(code: String) {
        // Get current selected session
        val selectedSessionIndex = spinnerSessions.selectedItemPosition
        val selectedSession = if (selectedSessionIndex >= 0 && selectedSessionIndex < sessions.size) {
            sessions[selectedSessionIndex]
        } else {
            currentSession
        }

        // Process verification
        viewModel.verifyAttendance(code, selectedSession.time_slot)

        // Navigate to result screen
        val bundle = Bundle().apply {
            putString("code", code)
            putString("session", selectedSession.time_slot)
        }
        findNavController().navigate(
            R.id.action_scannerFragment_to_verificationResultFragment as Int,
            bundle
        )
    }

    private fun showManualEntryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_manual_code_entry, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter Code Manually")
            .setView(dialogView)
            .setPositiveButton("Verify") { dialog, _ ->
                val codeInput = dialogView.findViewById<EditText>(R.id.etCode)
                val code = codeInput.text.toString().trim()

                if (code.isNotEmpty()) {
                    handleScannedCode(code)
                } else {
                    Toast.makeText(context, "Please enter a valid code", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
}