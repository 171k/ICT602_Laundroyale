package com.example.ictmobile.ui.customer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ictmobile.adapters.OrderAdapter
import com.example.ictmobile.databinding.ActivityOrderHistoryBinding
import com.example.ictmobile.models.Order
import com.example.ictmobile.services.FirebaseService

class OrderHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderHistoryBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        loadOrders()
    }
    
    private fun loadOrders() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        android.util.Log.d("OrderHistory", "Loading orders for user: ${currentUser.uid}")
        
        firebaseService.getOrders(currentUser.uid)
            .addOnSuccessListener { orders ->
                binding.progressBar.visibility = android.view.View.GONE
                android.util.Log.d("OrderHistory", "Loaded ${orders.size} orders")
                if (orders.isEmpty()) {
                    android.util.Log.w("OrderHistory", "No orders found for user")
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.rvOrders.visibility = android.view.View.GONE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                    binding.rvOrders.visibility = android.view.View.VISIBLE
                    val adapter = OrderAdapter(orders) { order ->
                        val intent = Intent(this, OrderDetailActivity::class.java)
                        intent.putExtra("order_id", order.id)
                        startActivity(intent)
                    }
                    binding.rvOrders.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                android.util.Log.e("OrderHistory", "Failed to load orders: ${exception.message}", exception)
                Toast.makeText(this, "Failed to load orders: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}
