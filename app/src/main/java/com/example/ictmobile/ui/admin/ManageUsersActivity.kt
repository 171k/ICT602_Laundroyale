package com.example.ictmobile.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ictmobile.adapters.UserAdapter
import com.example.ictmobile.databinding.ActivityManageUsersBinding
import com.example.ictmobile.models.User
import com.example.ictmobile.services.FirebaseService

class ManageUsersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageUsersBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        loadUsers()
    }
    
    private fun loadUsers() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.getAllUsers()
            .addOnSuccessListener { users ->
                binding.progressBar.visibility = android.view.View.GONE
                if (users.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.rvUsers.visibility = android.view.View.GONE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                    binding.rvUsers.visibility = android.view.View.VISIBLE
                    val adapter = UserAdapter(users) { user ->
                        showUserOptions(user)
                    }
                    binding.rvUsers.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load users: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showUserOptions(user: User) {
        AlertDialog.Builder(this)
            .setTitle(user.name)
            .setItems(arrayOf("View Details", "Delete User")) { _, which ->
                when (which) {
                    0 -> showUserDetails(user)
                    1 -> deleteUser(user)
                }
            }
            .show()
    }
    
    private fun showUserDetails(user: User) {
        AlertDialog.Builder(this)
            .setTitle("User Details")
            .setMessage("Name: ${user.name}\nEmail: ${user.email}\nUsername: ${user.username}\nPhone: ${user.phone}\nRole: ${user.role}")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun deleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                binding.progressBar.visibility = android.view.View.VISIBLE
                firebaseService.deleteUser(user.id)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Failed to delete user: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
