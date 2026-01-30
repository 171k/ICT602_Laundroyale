package com.example.ictmobile.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.R
import com.example.ictmobile.services.FirebaseService
import com.example.ictmobile.ui.auth.LoginActivity
import com.example.ictmobile.ui.customer.DashboardActivity
import com.example.ictmobile.ui.admin.AdminDashboardActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val firebaseService = FirebaseService.getInstance()
            val currentUser = firebaseService.getCurrentUser()
            
            if (currentUser != null) {
                // User is logged in, check role and navigate accordingly
                firebaseService.getCurrentUserData()
                    .addOnSuccessListener { user ->
                        val intent = if (user.isAdmin()) {
                            Intent(this, AdminDashboardActivity::class.java)
                        } else {
                            Intent(this, DashboardActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        // If getting user data fails, go to login
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            } else {
                // No user logged in, go to login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }, 2000) // 2 second splash screen
    }
}
