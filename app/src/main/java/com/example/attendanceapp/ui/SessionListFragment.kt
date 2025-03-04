package com.example.attendanceapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendanceapp.R
import com.example.attendanceapp.viewmodel.AttendanceViewModel

class SessionListFragment : Fragment() {

    private lateinit var viewModel: AttendanceViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_session_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // For now, just a basic implementation to avoid crashes
        // We'll implement the full functionality later

        view.findViewById<TextView>(R.id.tvSessionsTitle)?.text = "Available Sessions"
        view.findViewById<TextView>(R.id.tvDay)?.text = "Day: Saturday"

        // Set up radio button listeners
        val radioGroup = view.findViewById<RadioGroup>(R.id.rgDays)
        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSaturday -> {
                    // Load Saturday sessions
                }
                R.id.rbSunday -> {
                    // Load Sunday sessions
                }
            }
        }
    }
}