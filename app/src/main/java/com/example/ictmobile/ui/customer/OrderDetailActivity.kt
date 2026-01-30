package com.example.ictmobile.ui.customer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.databinding.ActivityOrderDetailBinding
import com.example.ictmobile.models.Order
import com.example.ictmobile.services.FirebaseService
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderDetailBinding
    private val firebaseService = FirebaseService.getInstance()
    private var order: Order? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val orderId = intent.getStringExtra("order_id")
        if (orderId != null) {
            loadOrder(orderId)
            startProgressUpdates()
        } else {
            Toast.makeText(this, "No order found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun loadOrder(orderId: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.getOrderById(orderId)
            .addOnSuccessListener { order ->
                this.order = order
                binding.progressBar.visibility = android.view.View.GONE
                displayOrderDetails()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load order: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    
    private fun displayOrderDetails() {
        order?.let { order ->
            binding.tvMachineName.text = order.machineName
            binding.tvStatus.text = "Status: ${order.status.capitalize()}"
            binding.tvProgress.text = "Progress: ${order.progress}"
            binding.tvStartTime.text = "Start: ${dateFormat.format(order.startTime)}"
            binding.tvEndTime.text = "End: ${dateFormat.format(order.endTime)}"
            binding.tvTotalAmount.text = "RM ${String.format("%.2f", order.totalAmount)}"
            
            order.timeRemaining?.let { minutes ->
                binding.tvTimeRemaining.text = "Time Remaining: $minutes minutes"
                binding.tvTimeRemaining.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvTimeRemaining.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun startProgressUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                order?.let { order ->
                    // Update progress and time remaining
                    binding.tvProgress.text = "Progress: ${order.progress}"
                    order.timeRemaining?.let { minutes ->
                        binding.tvTimeRemaining.text = "Time Remaining: $minutes minutes"
                    }
                }
                handler.postDelayed(this, 60000) // Update every minute
            }
        }
        handler.post(updateRunnable!!)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
}
