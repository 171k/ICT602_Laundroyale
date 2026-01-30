package com.example.ictmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ictmobile.R
import com.example.ictmobile.models.Machine

class MachineAdapter(
    private val machines: List<Machine>,
    private val onMachineClick: (Machine) -> Unit
) : RecyclerView.Adapter<MachineAdapter.MachineViewHolder>() {

    class MachineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val machineName: TextView = itemView.findViewById(R.id.tvMachineName)
        val machineStatus: TextView = itemView.findViewById(R.id.tvMachineStatus)
        val machineImage: ImageView = itemView.findViewById(R.id.ivMachineImage)
        val cardView: View = itemView.findViewById(R.id.machineCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MachineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_machine, parent, false)
        return MachineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MachineViewHolder, position: Int) {
        val machine = machines[position]
        // Display machine name - prioritize machineName, fallback to formatted ID
        val displayName = when {
            machine.machineName.isNotEmpty() -> machine.machineName
            machine.id.isNotEmpty() -> {
                // Format ID like "dryer_1" -> "Dryer 1" or "washer_1" -> "Washer 1"
                val formatted = machine.id.replace("_", " ").split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
                formatted
            }
            else -> "Machine ${position + 1}"
        }
        holder.machineName.text = displayName
        holder.machineName.visibility = View.VISIBLE
        
        val isAvailable = machine.status == "available"
        holder.machineStatus.text = if (isAvailable) "AVAILABLE" else "UNAVAILABLE"
        holder.machineStatus.setTextColor(
            if (isAvailable) {
                holder.itemView.context.getColor(R.color.green)
            } else {
                holder.itemView.context.getColor(R.color.red_accent)
            }
        )
        
        // Set machine image based on type
        val imageRes = when (machine.type) {
            "washer" -> R.drawable.washingmachine
            "dryer" -> R.drawable.dryer
            else -> R.drawable.washingmachine
        }
        holder.machineImage.setImageResource(imageRes)
        
        // Enable/disable click based on availability
        holder.cardView.isEnabled = isAvailable
        holder.cardView.alpha = if (isAvailable) 1.0f else 0.5f
        
        holder.cardView.setOnClickListener {
            if (isAvailable) {
                onMachineClick(machine)
            }
        }
    }

    override fun getItemCount() = machines.size
}
