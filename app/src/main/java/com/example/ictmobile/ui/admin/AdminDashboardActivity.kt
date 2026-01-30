package com.example.ictmobile.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.databinding.ActivityAdminDashboardBinding
import com.example.ictmobile.services.FirebaseService
import com.example.ictmobile.ui.auth.LoginActivity

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        loadAnalytics()
    }
    
    private fun setupClickListeners() {
        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        
        binding.btnManageMachines.setOnClickListener {
            startActivity(Intent(this, ManageMachinesActivity::class.java))
        }
        
        binding.btnAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        
        binding.btnLogout.setOnClickListener {
            firebaseService.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
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
                }
                
                orders?.let {
                    binding.tvTotalOrders.text = "${it["total"]}"
                    binding.tvMonthlyOrders.text = "${it["monthly"]}"
                }
                
                users?.let {
                    binding.tvTotalUsers.text = "${it["total"]}"
                    binding.tvActiveUsers.text = "${it["active_this_month"]}"
                }
                
                machines?.let {
                    binding.tvTotalMachines.text = "${it["total"]}"
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load analytics: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
