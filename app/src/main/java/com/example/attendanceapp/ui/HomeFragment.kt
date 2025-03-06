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
        tvSessionTime = view.findViewById(R.id.tvSessionTime)
        rvRecentScans = view.findViewById(R.id.rvRecentScans)
        tvNoScans = view.findViewById(R.id.tvNoScans)
        progressBar = view.findViewById(R.id.progressBar)

        // Initialize current session view - might be hidden in some layouts
        try {
            tvCurrentSession = view.findViewById(R.id.tvCurrentSession)
            // Hide the duplicate current session text view
            tvCurrentSession.visibility = View.GONE
        } catch (e: Exception) {
            Log.d("HomeFragment", "Current session view not found or already removed")
        }

        // Initialize stats views
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
                tvSessionTime.text = data.current_session.time_slot
                // Only set tvCurrentSession if it's still visible and initialized
                if (::tvCurrentSession.isInitialized && tvCurrentSession.visibility == View.VISIBLE) {
                    tvCurrentSession.text = data.current_session.time_slot
                }
            } else {
                tvSessionTime.text = "--:-- - --:--"
                if (::tvCurrentSession.isInitialized && tvCurrentSession.visibility == View.VISIBLE) {
                    tvCurrentSession.text = "No current session"
                }
            }

            // Default attendance rate - can be adjusted based on your needs
            if (::tvAttendanceRate.isInitialized) {
                tvAttendanceRate.text = "92%"
            }
        }

        // Observe local scan history for the scan list
        viewModel.localScanHistory.observe(viewLifecycleOwner) { scansList ->
            // Update recent scans UI
            if (scansList.isEmpty()) {
                rvRecentScans.visibility = View.GONE
                tvNoScans.visibility = View.VISIBLE
                // Set total scans to 0 if there are no scans
                if (::tvTotalScansCount.isInitialized) {
                    tvTotalScansCount.text = "0"
                }
            } else {
                rvRecentScans.visibility = View.VISIBLE
                tvNoScans.visibility = View.GONE
                recentScansAdapter.submitList(scansList)

                // Update total scans count based on actual scan history size
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