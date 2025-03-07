package com.example.attendanceapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.attendanceapp.R
import com.example.attendanceapp.models.VerificationResponse
import com.example.attendanceapp.viewmodel.AttendanceViewModel

class VerificationResultFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel

    // Views
    private lateinit var ivResultIcon: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvMessage: TextView
    private lateinit var tvParticipantName: TextView
    private lateinit var tvParticipantId: TextView
    private lateinit var btnDone: Button
    private lateinit var btnScanAnother: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[AttendanceViewModel::class.java]

        // Initialize views
        // Look for ivResultIcon inside the cardResultIcon container
        val cardResultIcon = view.findViewById<View>(R.id.cardResultIcon)
        ivResultIcon = cardResultIcon.findViewById(R.id.ivResultIcon) ?: view.findViewById(R.id.ivResultIcon)

        tvStatus = view.findViewById(R.id.tvStatus)
        tvMessage = view.findViewById(R.id.tvMessage)
        tvParticipantName = view.findViewById(R.id.tvParticipantName)
        tvParticipantId = view.findViewById(R.id.tvParticipantId)
        btnDone = view.findViewById(R.id.btnDone)
        btnScanAnother = view.findViewById(R.id.btnScanAnother)

        // Set up button click listeners
        btnDone.setOnClickListener {
            findNavController().navigate(R.id.action_verificationResultFragment_to_homeFragment)
        }

        btnScanAnother.setOnClickListener {
            findNavController().navigate(R.id.action_verificationResultFragment_to_scannerFragment)
        }

        // Observe ViewModel
        viewModel.verificationResult.observe(viewLifecycleOwner) { result: VerificationResponse ->
            updateUI(result.success, result.message, result.participant?.name, result.participant?.id)
        }

        // If we have arguments, update UI directly (for when coming from scanner)
        arguments?.let { args ->
            // This would be used if we want to show any data from the arguments
            // before the API call completes
        }
    }

    private fun updateUI(isSuccess: Boolean, message: String, name: String?, id: String?) {
        if (isSuccess) {
            try {
                ivResultIcon.setImageResource(R.drawable.ic_check)
                ivResultIcon.setBackgroundResource(R.drawable.bg_circle_success)
            } catch (e: Exception) {
                // Fallback to system icon if custom icons aren't available
                ivResultIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            }
            tvStatus.text = "Success"
            tvStatus.setTextColor(resources.getColor(R.color.success, null))
        } else {
            try {
                ivResultIcon.setImageResource(R.drawable.ic_error)
                ivResultIcon.setBackgroundResource(R.drawable.bg_circle_error)
            } catch (e: Exception) {
                // Fallback to system icon if custom icons aren't available
                ivResultIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            }
            tvStatus.text = "Error"
            tvStatus.setTextColor(resources.getColor(R.color.error, null))
        }

        tvMessage.text = message

        if (name != null) {
            tvParticipantName.text = name
        } else {
            tvParticipantName.text = "Unknown"
        }

        if (id != null) {
            tvParticipantId.text = id
        } else {
            tvParticipantId.text = "Unknown"
        }
    }
}