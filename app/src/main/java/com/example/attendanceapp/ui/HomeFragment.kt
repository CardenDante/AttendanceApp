package com.example.attendanceapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendanceapp.R
import com.example.attendanceapp.adapters.RecentScansAdapter
import com.example.attendanceapp.models.ScanEntry
import com.example.attendanceapp.models.ScannerDataResponse
import com.example.attendanceapp.viewmodel.AttendanceViewModel

class HomeFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var recentScansAdapter: RecentScansAdapter

    // Views
    private lateinit var tvDay: TextView
    private lateinit var tvCurrentSession: TextView
    private lateinit var rvRecentScans: RecyclerView
    private lateinit var tvNoScans: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[AttendanceViewModel::class.java]

        // Initialize views
        tvDay = view.findViewById(R.id.tvDay)
        tvCurrentSession = view.findViewById(R.id.tvCurrentSession)
        rvRecentScans = view.findViewById(R.id.rvRecentScans)
        tvNoScans = view.findViewById(R.id.tvNoScans)
        progressBar = view.findViewById(R.id.progressBar)

        // Set up adapter
        recentScansAdapter = RecentScansAdapter()
        rvRecentScans.layoutManager = LinearLayoutManager(requireContext())
        rvRecentScans.adapter = recentScansAdapter

        // Set up click listeners
        view.findViewById<Button>(R.id.btnScan).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scannerFragment as Int)
        }

        view.findViewById<Button>(R.id.btnSessions).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_sessionListFragment as Int)
        }

        view.findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            viewModel.clearHistory()
        }

        // Observe ViewModel
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe scanner data for day and session info
        viewModel.scannerData.observe(viewLifecycleOwner) { data: ScannerDataResponse ->
            // Update day and current session
            tvDay.text = "Today: ${data.day_name}"

            if (data.current_session != null) {
                tvCurrentSession.text = data.current_session.time_slot
            } else {
                tvCurrentSession.text = "No current session"
            }
        }

        // Observe local scan history for the scan list
        viewModel.localScanHistory.observe(viewLifecycleOwner) { scansList ->
            // Log the data to debug
            Log.d("RecentScansDebug", "Local scan history received with ${scansList.size} scans")
            for (scan in scansList) {
                Log.d("RecentScansDebug", "Scan: ${scan.timestamp} - ${scan.name} - ${scan.status}")
            }

            // Update recent scans UI
            if (scansList.isEmpty()) {
                rvRecentScans.visibility = View.GONE
                tvNoScans.visibility = View.VISIBLE
            } else {
                rvRecentScans.visibility = View.VISIBLE
                tvNoScans.visibility = View.GONE
                recentScansAdapter.submitList(scansList)
            }
        }

        // Load data immediately when fragment is created
        viewModel.loadScannerData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data each time the fragment becomes visible
        viewModel.loadScannerData()
    }
}