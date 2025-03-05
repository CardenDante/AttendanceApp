package com.example.attendanceapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendanceapp.R
import com.example.attendanceapp.models.ScanEntry

class RecentScansAdapter : ListAdapter<ScanEntry, RecentScansAdapter.ScanViewHolder>(ScanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan, parent, false)
        return ScanViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvScanTime: TextView = itemView.findViewById(R.id.tvScanTime)
        private val tvScanStatus: TextView = itemView.findViewById(R.id.tvScanStatus)
        private val tvParticipantName: TextView = itemView.findViewById(R.id.tvParticipantName)
        private val tvParticipantId: TextView = itemView.findViewById(R.id.tvParticipantId)

        fun bind(scan: ScanEntry) {
            tvScanTime.text = scan.timestamp
            tvParticipantName.text = scan.name
            tvParticipantId.text = "ID: ${scan.id}"

            // Set status text
            tvScanStatus.text = scan.status

            // Set text color to white (using Color constant to avoid ambiguity)
            tvScanStatus.setTextColor(Color.WHITE)

            // Set background based on status
            tvScanStatus.setBackgroundResource(
                if (scan.status == "Correct") R.drawable.bg_status_success
                else R.drawable.bg_status_error
            )
        }
    }

    class ScanDiffCallback : DiffUtil.ItemCallback<ScanEntry>() {
        override fun areItemsTheSame(oldItem: ScanEntry, newItem: ScanEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanEntry, newItem: ScanEntry): Boolean {
            return oldItem == newItem
        }
    }
}