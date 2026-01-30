package com.example.ictmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ictmobile.R
import com.example.ictmobile.models.Voucher
import java.text.SimpleDateFormat
import java.util.*

class VoucherAdapter(
    private val vouchers: List<Voucher>,
    private val dateFormat: SimpleDateFormat
) : RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder>() {

    class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvType: TextView = itemView.findViewById(R.id.tvVoucherType)
        val tvExpires: TextView = itemView.findViewById(R.id.tvVoucherExpires)
        val tvCreated: TextView = itemView.findViewById(R.id.tvVoucherCreated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        val voucher = vouchers[position]
        holder.tvType.text = when (voucher.type) {
            "rm5_off" -> "RM5 OFF Voucher"
            else -> voucher.type
        }
        
        voucher.expiresAt?.let {
            holder.tvExpires.text = "Expires: ${dateFormat.format(it)}"
            holder.tvExpires.visibility = View.VISIBLE
        } ?: run {
            holder.tvExpires.visibility = View.GONE
        }
        
        holder.tvCreated.text = "Created: ${dateFormat.format(voucher.createdAt)}"
    }

    override fun getItemCount() = vouchers.size
}
