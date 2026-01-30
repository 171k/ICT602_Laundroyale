package com.example.ictmobile.ui.customer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ictmobile.adapters.VoucherAdapter
import com.example.ictmobile.databinding.ActivityVouchersBinding
import com.example.ictmobile.models.Voucher
import com.example.ictmobile.services.FirebaseService
import java.text.SimpleDateFormat
import java.util.*

class VouchersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVouchersBinding
    private val firebaseService = FirebaseService.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVouchersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.rvAvailable.layoutManager = LinearLayoutManager(this)
        binding.rvUsed.layoutManager = LinearLayoutManager(this)
        binding.rvExpired.layoutManager = LinearLayoutManager(this)
        
        loadVouchers()
    }
    
    private fun loadVouchers() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.getVouchers(currentUser.uid)
            .addOnSuccessListener { vouchers ->
                binding.progressBar.visibility = android.view.View.GONE
                
                val available = vouchers.filter { it.isValid() }
                val used = vouchers.filter { it.used }
                val expired = vouchers.filter { !it.used && !it.isValid() }
                
                if (available.isEmpty()) {
                    binding.tvAvailableEmpty.visibility = android.view.View.VISIBLE
                    binding.rvAvailable.visibility = android.view.View.GONE
                } else {
                    binding.tvAvailableEmpty.visibility = android.view.View.GONE
                    binding.rvAvailable.visibility = android.view.View.VISIBLE
                    binding.rvAvailable.adapter = VoucherAdapter(available, dateFormat)
                }
                
                if (used.isEmpty()) {
                    binding.tvUsedEmpty.visibility = android.view.View.VISIBLE
                    binding.rvUsed.visibility = android.view.View.GONE
                } else {
                    binding.tvUsedEmpty.visibility = android.view.View.GONE
                    binding.rvUsed.visibility = android.view.View.VISIBLE
                    binding.rvUsed.adapter = VoucherAdapter(used, dateFormat)
                }
                
                if (expired.isEmpty()) {
                    binding.tvExpiredEmpty.visibility = android.view.View.VISIBLE
                    binding.rvExpired.visibility = android.view.View.GONE
                } else {
                    binding.tvExpiredEmpty.visibility = android.view.View.GONE
                    binding.rvExpired.visibility = android.view.View.VISIBLE
                    binding.rvExpired.adapter = VoucherAdapter(expired, dateFormat)
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load vouchers: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
