// ui/ScannerFragment.kt
package com.example.attendanceapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.attendanceapp.R
import com.example.attendanceapp.models.ScannerDataResponse
import com.example.attendanceapp.models.Session
import com.example.attendanceapp.viewmodel.AttendanceViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var spinnerSessions: Spinner
    private lateinit var sessionAdapter: ArrayAdapter<String>
    private lateinit var currentSession: Session
    private lateinit var sessions: List<Session>

    // Camera variables
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView

    // Sound variables
    private var successSound: MediaPlayer? = null
    private var errorSound: MediaPlayer? = null
    private var scanning = true // Flag to prevent multiple scans
    private var processingCode = false // Flag to prevent multiple verifications

    // Handler for delayed operations
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
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
        previewView = view.findViewById(R.id.previewView)
        spinnerSessions = view.findViewById(R.id.spinnerSessions)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize sounds
        successSound = MediaPlayer.create(context, R.raw.success_sound)
        errorSound = MediaPlayer.create(context, R.raw.error_sound)

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
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Image analysis
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
            if (scanning && !processingCode) {
                scanning = false // Prevent multiple scans
                processingCode = true // Prevent multiple verifications
                handleScannedCode(qrCode)
            }
        })

        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind any bound use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("ScannerFragment", "Use case binding failed", e)
        }
    }

    private inner class QrCodeAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
        private val reader = MultiFormatReader()

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val width = image.width
            val height = image.height

            val source = PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decode(binaryBitmap)
                onQrCodeScanned(result.text)
            } catch (e: Exception) {
                // QR code not detected - this is expected for most frames
            } finally {
                image.close()
            }
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }
    }

    private fun handleScannedCode(code: String) {
        // Check if fragment is in a valid state
        if (!isAdded || view == null) {
            Log.e("ScannerFragment", "Fragment not in valid state for handling code")
            processingCode = false
            scanning = true
            return
        }

        activity?.runOnUiThread {
            try {
                // Get current selected session
                val selectedSessionIndex = spinnerSessions.selectedItemPosition
                val selectedSession = if (selectedSessionIndex >= 0 && selectedSessionIndex < sessions.size) {
                    sessions[selectedSessionIndex]
                } else {
                    currentSession
                }

                // Process verification without observer
                viewModel.verifyAttendance(code, selectedSession.time_slot)

                // Use a handler to delay navigation and play sound
                handler.postDelayed({
                    try {
                        if (!isAdded || view == null) {
                            Log.e("ScannerFragment", "Fragment detached during delay")
                            return@postDelayed
                        }

                        // Play appropriate sound
                        if (viewModel.verificationResult.value?.success == true) {
                            successSound?.start()
                        } else {
                            errorSound?.start()
                        }

                        // Short delay to let sound play before navigation
                        handler.postDelayed({
                            try {
                                if (isAdded && view != null) {
                                    val bundle = Bundle().apply {
                                        putString("code", code)
                                        putString("session", selectedSession.time_slot)
                                    }
                                    findNavController().navigate(R.id.verificationResultFragment, bundle)
                                }
                            } catch (e: Exception) {
                                Log.e("ScannerFragment", "Error during navigation", e)
                            } finally {
                                processingCode = false
                            }
                        }, 200)
                    } catch (e: Exception) {
                        Log.e("ScannerFragment", "Error playing sound", e)
                        processingCode = false
                    }
                }, 500) // Delay to ensure verification has completed
            } catch (e: Exception) {
                Log.e("ScannerFragment", "Error handling scanned code", e)
                processingCode = false
                scanning = true
            }
        }
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
                    if (!processingCode) {
                        processingCode = true
                        handleScannedCode(code)
                    }
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

    override fun onPause() {
        super.onPause()
        // Reset scanning flag
        scanning = true
        processingCode = false
        // Remove any pending handlers
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        // Reset flags when returning to the scanner
        scanning = true
        processingCode = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove callbacks to prevent leaks
        handler.removeCallbacksAndMessages(null)
        // Release resources
        cameraExecutor.shutdown()
        successSound?.release()
        errorSound?.release()
    }
}