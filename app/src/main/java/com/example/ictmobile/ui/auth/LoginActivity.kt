package com.example.ictmobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.R
import com.example.ictmobile.databinding.ActivityLoginBinding
import com.example.ictmobile.services.FirebaseService
import com.example.ictmobile.ui.customer.DashboardActivity
import com.example.ictmobile.ui.admin.AdminDashboardActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.btnLogin.isEnabled = false
            binding.progressBar.visibility = android.view.View.VISIBLE
            
            firebaseService.login(email, password)
                .addOnSuccessListener { user ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    val intent = if (user.isAdmin()) {
                        Intent(this, AdminDashboardActivity::class.java)
                    } else {
                        Intent(this, DashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, "Login failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
