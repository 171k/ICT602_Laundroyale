package com.example.ictmobile.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val phone: String = "",
    val role: String = "customer",
    val profilePicture: String = "king.png"
) {
    fun isAdmin(): Boolean = role == "admin"
    fun isCustomer(): Boolean = role == "customer"
    
    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                username = map["username"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                role = map["role"] as? String ?: "customer",
                profilePicture = map["profile_picture"] as? String ?: "king.png"
            )
        }
        
        fun toMap(user: User): Map<String, Any> {
            return mapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "username" to user.username,
                "phone" to user.phone,
                "role" to user.role,
                "profile_picture" to user.profilePicture
            )
        }
    }
}
