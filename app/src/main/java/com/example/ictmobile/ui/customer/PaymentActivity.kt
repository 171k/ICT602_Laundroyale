package com.example.ictmobile.ui.customer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.databinding.ActivityPaymentBinding
import com.example.ictmobile.models.Order
import com.example.ictmobile.models.Voucher
import com.example.ictmobile.services.FirebaseService
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private val firebaseService = FirebaseService.getInstance()
    private var order: Order? = null
    private var selectedVoucher: Voucher? = null
    private var availableVouchers: List<Voucher> = emptyList()
    private val malaysiaTimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
        timeZone = malaysiaTimeZone
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val orderId = intent.getStringExtra("order_id")
        if (orderId != null) {
            loadOrder(orderId)
        } else {
            Toast.makeText(this, "No order found", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        setupPaymentMethodSpinner()
        loadVouchers()
        setupClickListeners()
    }
    
    private fun setupPaymentMethodSpinner() {
        val methods = arrayOf("Credit Card", "Debit Card", "E-Wallet")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, methods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaymentMethod.adapter = adapter
    }
    
    private fun loadOrder(orderId: String, retryCount: Int = 0) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        android.util.Log.d("PaymentActivity", "Loading order with ID: $orderId (attempt ${retryCount + 1})")
        
        firebaseService.getOrderById(orderId)
            .addOnSuccessListener { order ->
                android.util.Log.d("PaymentActivity", "Order loaded successfully: ${order.id}, paymentId: ${order.paymentId}")
                this.order = order
                binding.progressBar.visibility = android.view.View.GONE
                
                // If paymentId is missing, wait a bit and retry (payment might still be creating)
                if (order.paymentId.isBlank() && retryCount < 3) {
                    android.util.Log.w("PaymentActivity", "Order paymentId is empty, retrying in 1 second...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadOrder(orderId, retryCount + 1)
                    }, 1000)
                } else {
                    displayOrderDetails()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("PaymentActivity", "Failed to load order: ${exception.message}", exception)
                
                // Retry if it's a temporary error and we haven't exceeded retry limit
                if (retryCount < 2 && (exception.message?.contains("not found") == true || 
                    exception.message?.contains("permission") == true)) {
                    android.util.Log.w("PaymentActivity", "Retrying order load in 1 second...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadOrder(orderId, retryCount + 1)
                    }, 1000)
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Failed to load order: ${exception.message}", Toast.LENGTH_LONG).show()
                    // Wait a bit before finishing to show the error message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 2000)
                }
            }
    }
    
    private fun loadVouchers() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null) {
            firebaseService.getVouchers(currentUser.uid)
                .addOnSuccessListener { vouchers ->
                    availableVouchers = vouchers.filter { it.isValid() }
                    updateVoucherSpinner()
                }
                .addOnFailureListener { exception ->
                    // Log error but don't block payment - vouchers are optional
                    android.util.Log.w("PaymentActivity", "Failed to load vouchers: ${exception.message}")
                }
        }
    }
    
    private fun updateVoucherSpinner() {
        val voucherOptions = mutableListOf<String>("No Voucher")
        voucherOptions.addAll(availableVouchers.map { "RM5 OFF Voucher" })
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voucherOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVoucher.adapter = adapter
        
        binding.spinnerVoucher.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedVoucher = if (position > 0) availableVouchers[position - 1] else null
                updateTotalAmount()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
    
    private fun displayOrderDetails() {
        order?.let { order ->
            binding.tvMachineName.text = order.machineName
            // Fix deprecated capitalize() - use replaceFirstChar instead
            val tempText = order.temperature.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            binding.tvTemperature.text = "Temperature: $tempText"
            binding.tvStartTime.text = "Start: ${dateFormat.format(order.startTime)}"
            binding.tvEndTime.text = "End: ${dateFormat.format(order.endTime)}"
            updateTotalAmount()
        }
    }
    
    private fun updateTotalAmount() {
        order?.let { order ->
            var total = order.totalAmount
            selectedVoucher?.let { voucher ->
                if (voucher.type == "rm5_off") {
                    total = (total - 5.0).coerceAtLeast(0.0)
                }
            }
            binding.tvTotalAmount.text = "RM ${String.format("%.2f", total)}"
        }
    }
    
    private fun setupClickListeners() {
        binding.btnPayNow.setOnClickListener { processPayment() }
        binding.btnCancel.setOnClickListener { finish() }
    }
    
    private fun processPayment() {
        val order = this.order ?: return
        val paymentMethod = binding.spinnerPaymentMethod.selectedItem.toString().lowercase().replace(" ", "_")
        
        if (paymentMethod == "select_payment_method") {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if payment ID exists
        if (order.paymentId.isBlank()) {
            Toast.makeText(this, "Payment information not found. Please try again.", Toast.LENGTH_SHORT).show()
            android.util.Log.e("PaymentActivity", "Order paymentId is empty for order: ${order.id}")
            return
        }
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnPayNow.isEnabled = false
        
        val voucherId = selectedVoucher?.id
        
        // Get payment ID from order
        android.util.Log.d("PaymentActivity", "Processing payment for order: ${order.id}, paymentId: ${order.paymentId}")
        firebaseService.getPaymentById(order.paymentId)
            .addOnSuccessListener { payment ->
                android.util.Log.d("PaymentActivity", "Payment retrieved: ${payment.id}, calling completePayment...")
                firebaseService.completePayment(payment.id, paymentMethod, voucherId)
                    .addOnSuccessListener {
                        android.util.Log.d("PaymentActivity", "Payment completed successfully!")
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnPayNow.isEnabled = true
                        Toast.makeText(this, "Payment completed successfully! Token awarded.", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to order history
                        val intent = android.content.Intent(this, OrderHistoryActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnPayNow.isEnabled = true
                        android.util.Log.e("PaymentActivity", "Payment completion failed: ${exception.message}", exception)
                        Toast.makeText(this, "Payment failed: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnPayNow.isEnabled = true
                android.util.Log.e("PaymentActivity", "Failed to get payment: ${exception.message}", exception)
                Toast.makeText(this, "Failed to process payment: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}
