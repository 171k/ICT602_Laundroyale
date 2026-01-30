package com.example.ictmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ictmobile.R
import com.example.ictmobile.models.Order
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMachineName: TextView = itemView.findViewById(R.id.tvMachineName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val cardView: View = itemView.findViewById(R.id.orderCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvMachineName.text = order.machineName
        holder.tvStatus.text = "Status: ${order.status.capitalize()}"
        holder.tvProgress.text = "Progress: ${order.progress}"
        holder.tvStartTime.text = "Start: ${dateFormat.format(order.startTime)}"
        holder.tvTotalAmount.text = "RM ${String.format("%.2f", order.totalAmount)}"
        
        holder.cardView.setOnClickListener {
            onOrderClick(order)
        }
    }

    override fun getItemCount() = orders.size
}
