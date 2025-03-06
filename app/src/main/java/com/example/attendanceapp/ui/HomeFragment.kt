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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendanceapp.R
import com.example.attendanceapp.adapters.RecentScansAdapter
import com.example.attendanceapp.models.ScannerDataResponse
import com.example.attendanceapp.viewmodel.AttendanceViewModel

class HomeFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var recentScansAdapter: RecentScansAdapter

    // Views
    private lateinit var tvDay: TextView
    private lateinit var tvCurrentSession: TextView
    private lateinit var tvSessionTime: TextView
    private lateinit var rvRecentScans: RecyclerView
    private lateinit var tvNoScans: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTotalScansCount: TextView
    private lateinit var tvAttendanceRate: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set custom title view in toolbar
        setupCustomToolbar()

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[AttendanceViewModel::class.java]

        // Initialize views
        tvDay = view.findViewById(R.id.tvDay)
        tvCurrentSession = view.findViewById(R.id.tvCurrentSession)
        tvSessionTime = view.findViewById(R.id.tvSessionTime)
        rvRecentScans = view.findViewById(R.id.rvRecentScans)
        tvNoScans = view.findViewById(R.id.tvNoScans)
        progressBar = view.findViewById(R.id.progressBar)

        // Initialize stats views (if they exist in your layout)
        try {
            tvTotalScansCount = view.findViewById(R.id.tvTotalScansCount)
            tvAttendanceRate = view.findViewById(R.id.tvAttendanceRate)
        } catch (e: Exception) {
            Log.d("HomeFragment", "Stats views not found in layout")
        }

        // Set up adapter
        recentScansAdapter = RecentScansAdapter()
        rvRecentScans.layoutManager = LinearLayoutManager(requireContext())
        rvRecentScans.adapter = recentScansAdapter

        // Set up click listeners
        view.findViewById<Button>(R.id.btnScan).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scannerFragment)
        }

        view.findViewById<Button>(R.id.btnSessions).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_sessionListFragment)
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
                tvSessionTime.text = data.current_session.time_slot
            } else {
                tvCurrentSession.text = "No current session"
                tvSessionTime.text = "--:-- - --:--"
            }

            // Update stats if available
            if (::tvTotalScansCount.isInitialized && ::tvAttendanceRate.isInitialized) {
                // Set static values for now - replace when you add these to your data model
                tvTotalScansCount.text = "128"
                tvAttendanceRate.text = "92%"
            }
        }

        // Observe local scan history for the scan list
        viewModel.localScanHistory.observe(viewLifecycleOwner) { scansList ->
            // Update recent scans UI
            if (scansList.isEmpty()) {
                rvRecentScans.visibility = View.GONE
                tvNoScans.visibility = View.VISIBLE
            } else {
                rvRecentScans.visibility = View.VISIBLE
                tvNoScans.visibility = View.GONE
                recentScansAdapter.submitList(scansList)

                // If you want to update total scans count based on scan history
                if (::tvTotalScansCount.isInitialized) {
                    tvTotalScansCount.text = scansList.size.toString()
                }
            }
        }

        // Load data immediately when fragment is created
        viewModel.loadScannerData()
    }

    private fun setupCustomToolbar() {
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            // Hide default title
            actionBar.setDisplayShowTitleEnabled(false)

            // Show custom view
            actionBar.setDisplayShowCustomEnabled(true)

            // Create and set custom view
            val customView = layoutInflater.inflate(R.layout.toolbar_home, null)
            actionBar.customView = customView
        }
    }

    override fun onDestroyView() {
        // Restore default title when leaving fragment
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayShowCustomEnabled(false)
        }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data each time the fragment becomes visible
        viewModel.loadScannerData()
    }
}