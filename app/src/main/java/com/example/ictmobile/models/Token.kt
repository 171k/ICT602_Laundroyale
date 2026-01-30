package com.example.ictmobile.models

data class Token(
    val id: String = "",
    val userId: String = "",
    val orderId: String? = null,
    val used: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): Token {
            val usedValue = map["used"]
            val used = when (usedValue) {
                is Boolean -> usedValue
                is Number -> usedValue.toInt() != 0
                else -> false
            }
            
            return Token(
                id = map["id"] as? String ?: "",
                userId = map["user_id"] as? String ?: "",
                orderId = map["order_id"] as? String,
                used = used
            )
        }
        
        fun toMap(token: Token): Map<String, Any> {
            val map = mutableMapOf<String, Any>(
                "user_id" to token.userId,
                "used" to token.used
            )
            
            token.orderId?.let { map["order_id"] = it }
            
            return map
        }
    }
}
