package com.example.ictmobile.ui.customer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ictmobile.databinding.ActivityDashboardBinding
import com.example.ictmobile.services.FirebaseService
import com.example.ictmobile.ui.auth.LoginActivity
import com.example.ictmobile.ui.customer.BookMachineActivity
import com.example.ictmobile.ui.customer.OrderHistoryActivity
import com.example.ictmobile.ui.customer.MinigameActivity
import com.example.ictmobile.ui.customer.VouchersActivity
import com.example.ictmobile.ui.customer.SettingsActivity
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val firebaseService = FirebaseService.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val malaysiaTimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadUserData()
        setupClickListeners()
        setupSidebar()
        startClock()
    }
    
    private fun startClock() {
        updateClock()
        handler.postDelayed(clockRunnable, 1000)
    }
    
    private fun updateClock() {
        try {
            val calendar = Calendar.getInstance(malaysiaTimeZone)
            
            // Format time as "HH : MM : SS" (with spaces around colons)
            val hours = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
            val minutes = String.format("%02d", calendar.get(Calendar.MINUTE))
            val seconds = String.format("%02d", calendar.get(Calendar.SECOND))
            binding.tvClockTime.text = "$hours : $minutes : $seconds"
            binding.tvClockTime.visibility = android.view.View.VISIBLE
            
            // Format date as "DD/MM/YYYY"
            val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val year = calendar.get(Calendar.YEAR)
            binding.tvClockDate.text = "$day/$month/$year"
            binding.tvClockDate.visibility = android.view.View.VISIBLE
        } catch (e: Exception) {
            android.util.Log.e("DashboardActivity", "Error updating clock: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
    }
    
    private fun setupSidebar() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.END)
        }
        
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                com.example.ictmobile.R.id.nav_dashboard -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                }
                com.example.ictmobile.R.id.nav_book_machine -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                    startActivity(Intent(this, BookMachineActivity::class.java))
                }
                com.example.ictmobile.R.id.nav_order_history -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                    startActivity(Intent(this, OrderHistoryActivity::class.java))
                }
                com.example.ictmobile.R.id.nav_minigame -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                    startActivity(Intent(this, MinigameActivity::class.java))
                }
                com.example.ictmobile.R.id.nav_vouchers -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                    startActivity(Intent(this, VouchersActivity::class.java))
                }
                com.example.ictmobile.R.id.nav_settings -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                com.example.ictmobile.R.id.nav_logout -> {
                    binding.drawerLayout.closeDrawer(Gravity.END)
                    firebaseService.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            true
        }
    }
    
    private fun loadUserData() {
        firebaseService.getCurrentUserData()
            .addOnSuccessListener { user ->
                binding.tvWelcome.text = "Welcome back, ${user.name}!"
                loadTokenCount()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadTokenCount() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null) {
            firebaseService.getAvailableTokensCount(currentUser.uid)
                .addOnSuccessListener { count ->
                    binding.tvTokens.text = "Tokens: $count"
                }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBookMachine.setOnClickListener {
            startActivity(Intent(this, BookMachineActivity::class.java))
        }
        
        binding.btnOrderHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }
        
        binding.btnMinigame.setOnClickListener {
            startActivity(Intent(this, MinigameActivity::class.java))
        }
    }
}
