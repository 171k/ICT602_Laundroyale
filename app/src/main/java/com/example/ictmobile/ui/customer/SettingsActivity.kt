package com.example.ictmobile.ui.customer

import android.content.res.AssetManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ictmobile.adapters.ProfilePictureAdapter
import com.example.ictmobile.databinding.ActivitySettingsBinding
import com.example.ictmobile.models.User
import com.example.ictmobile.services.FirebaseService
import java.io.IOException

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val firebaseService = FirebaseService.getInstance()
    private var currentUser: User? = null
    private var availablePictures: List<String> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivitySettingsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            binding.btnBack.setOnClickListener {
                finish()
            }
            
            loadAvailablePictures()
            loadUserData()
            setupClickListeners()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading settings: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun loadAvailablePictures() {
        try {
            val assetManager: AssetManager = assets
            val files = assetManager.list("profilepictures")
            availablePictures = files?.filter { it.endsWith(".png", ignoreCase = true) }?.sorted() ?: emptyList()
            
            binding.rvProfilePictures.layoutManager = GridLayoutManager(this, 4)
            binding.rvProfilePictures.adapter = ProfilePictureAdapter(availablePictures) { pictureName ->
                currentUser?.let { user ->
                    val updatedUser = user.copy(profilePicture = pictureName)
                    updateProfilePicture(updatedUser)
                }
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to load profile pictures", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadUserData() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.getCurrentUserData()
            .addOnSuccessListener { user ->
                currentUser = user
                binding.progressBar.visibility = android.view.View.GONE
                displayUserData()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun displayUserData() {
        currentUser?.let { user ->
            binding.etName.setText(user.name)
            binding.etEmail.setText(user.email)
            binding.etUsername.setText(user.username)
            binding.etPhone.setText(user.phone)
            
            // Load current profile picture
            loadProfilePicture(user.profilePicture)
        }
    }
    
    private fun loadProfilePicture(pictureName: String) {
        try {
            val inputStream = assets.open("profilepictures/$pictureName")
            binding.ivCurrentProfile.setImageBitmap(android.graphics.BitmapFactory.decodeStream(inputStream))
            inputStream.close()
        } catch (e: Exception) {
            // Use default if not found
            try {
                val defaultStream = assets.open("profilepictures/king.png")
                binding.ivCurrentProfile.setImageBitmap(android.graphics.BitmapFactory.decodeStream(defaultStream))
                defaultStream.close()
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener { saveSettings() }
    }
    
    private fun updateProfilePicture(user: User) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.updateUser(user)
            .addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                currentUser = user
                loadProfilePicture(user.profilePicture)
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to update profile picture: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun saveSettings() {
        val user = currentUser ?: return
        
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        
        val oldPassword = binding.etOldPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        if (newPassword.isNotEmpty()) {
            if (newPassword.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }
            // Note: Password update requires Firebase Auth, which needs re-authentication
            // For now, we'll skip password update in settings
            Toast.makeText(this, "Password update requires re-authentication. Please use Firebase Console.", Toast.LENGTH_LONG).show()
        }
        
        val updatedUser = user.copy(
            name = name,
            email = email,
            username = username,
            phone = phone
        )
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.updateUser(updatedUser)
            .addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                currentUser = updatedUser
                Toast.makeText(this, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to update settings: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
