package com.example.ictmobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.R
import com.example.ictmobile.databinding.ActivityRegisterBinding
import com.example.ictmobile.services.FirebaseService
import com.example.ictmobile.ui.customer.DashboardActivity

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.btnRegister.isEnabled = false
            binding.progressBar.visibility = android.view.View.VISIBLE
            
            firebaseService.register(email, password, name, phone)
                .addOnSuccessListener { user ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
}
