package com.example.ictmobile.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.databinding.ActivityAnalyticsBinding
import com.example.ictmobile.services.FirebaseService

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalyticsBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.getAnalytics()
            .addOnSuccessListener { analytics ->
                binding.progressBar.visibility = android.view.View.GONE
                
                val revenue = analytics["revenue"] as? Map<*, *>
                val orders = analytics["orders"] as? Map<*, *>
                val users = analytics["users"] as? Map<*, *>
                val machines = analytics["machines"] as? Map<*, *>
                
                revenue?.let {
                    binding.tvTotalRevenue.text = "RM ${String.format("%.2f", (it["total"] as? Number)?.toDouble() ?: 0.0)}"
                    binding.tvMonthlyRevenue.text = "RM ${String.format("%.2f", (it["monthly"] as? Number)?.toDouble() ?: 0.0)}"
                    binding.tvYearlyRevenue.text = "RM ${String.format("%.2f", (it["yearly"] as? Number)?.toDouble() ?: 0.0)}"
                }
                
                orders?.let {
                    binding.tvTotalOrders.text = "${it["total"]}"
                    binding.tvMonthlyOrders.text = "${it["monthly"]}"
                    binding.tvYearlyOrders.text = "${it["yearly"]}"
                }
                
                users?.let {
                    binding.tvTotalUsers.text = "${it["total"]}"
                    binding.tvActiveUsers.text = "${it["active_this_month"]}"
                }
                
                machines?.let {
                    binding.tvTotalMachines.text = "${it["total"]}"
                    binding.tvWashers.text = "${it["washers"]}"
                    binding.tvDryers.text = "${it["dryers"]}"
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load analytics: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
