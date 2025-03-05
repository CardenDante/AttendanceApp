package com.example.attendanceapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendanceapp.R
import com.example.attendanceapp.adapters.SessionAdapter
import com.example.attendanceapp.viewmodel.AttendanceViewModel

class SessionListFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var rvSessions: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoSessions: TextView
    private lateinit var rgDays: RadioGroup
    private lateinit var rbSaturday: RadioButton
    private lateinit var rbSunday: RadioButton
    private lateinit var tvDayInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_session_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[AttendanceViewModel::class.java]

        // Initialize views
        rvSessions = view.findViewById(R.id.rvSessions)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoSessions = view.findViewById(R.id.tvNoSessions)
        rgDays = view.findViewById(R.id.rgDays)
        rbSaturday = view.findViewById(R.id.rbSaturday)
        rbSunday = view.findViewById(R.id.rbSunday)
        tvDayInfo = view.findViewById(R.id.tvDay)

        // Set up recycler view
        sessionAdapter = SessionAdapter()
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionAdapter

        // Set up day selection
        rgDays.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSaturday -> {
                    tvDayInfo.text = "Day: Saturday"
                    viewModel.loadAvailableSessions("Saturday")
                }
                R.id.rbSunday -> {
                    tvDayInfo.text = "Day: Sunday"
                    viewModel.loadAvailableSessions("Sunday")
                }
            }
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

        viewModel.sessions.observe(viewLifecycleOwner) { sessionsList ->
            Log.d("SessionsDebug", "Received ${sessionsList.size} sessions")

            if (sessionsList.isEmpty()) {
                rvSessions.visibility = View.GONE
                tvNoSessions.visibility = View.VISIBLE
            } else {
                rvSessions.visibility = View.VISIBLE
                tvNoSessions.visibility = View.GONE
                sessionAdapter.submitList(sessionsList)
            }
        }

        // Load initial data (Saturday by default)
        rbSaturday.isChecked = true
        viewModel.loadAvailableSessions("Saturday")
    }
}