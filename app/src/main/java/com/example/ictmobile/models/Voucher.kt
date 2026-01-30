package com.example.ictmobile.models

import com.google.firebase.Timestamp
import java.util.Date

data class Voucher(
    val id: String = "",
    val userId: String = "",
    val type: String = "rm5_off", // "rm5_off"
    val used: Boolean = false,
    val orderId: String? = null,
    val expiresAt: Date? = null,
    val createdAt: Date = Date()
) {
    fun isValid(): Boolean {
        if (used) return false
        if (expiresAt == null) return true
        return expiresAt.after(Date())
    }
    
    companion object {
        fun fromMap(map: Map<String, Any?>): Voucher {
            val usedValue = map["used"]
            val used = when (usedValue) {
                is Boolean -> usedValue
                is Number -> usedValue.toInt() != 0
                else -> false
            }
            
            val expiresAtTimestamp = map["expires_at"] as? Timestamp
            val createdAtTimestamp = map["created_at"] as? Timestamp
            
            return Voucher(
                id = map["id"] as? String ?: "",
                userId = map["user_id"] as? String ?: "",
                type = map["type"] as? String ?: "rm5_off",
                used = used,
                orderId = map["order_id"] as? String,
                expiresAt = expiresAtTimestamp?.toDate(),
                createdAt = createdAtTimestamp?.toDate() ?: Date()
            )
        }
        
        fun toMap(voucher: Voucher): Map<String, Any> {
            val map = mutableMapOf<String, Any>(
                "user_id" to voucher.userId,
                "type" to voucher.type,
                "used" to voucher.used,
                "created_at" to com.google.firebase.Timestamp(voucher.createdAt.time / 1000, ((voucher.createdAt.time % 1000) * 1000000).toInt())
            )
            
            voucher.orderId?.let { map["order_id"] = it }
            voucher.expiresAt?.let { map["expires_at"] = com.google.firebase.Timestamp(it.time / 1000, ((it.time % 1000) * 1000000).toInt()) }
            
            return map
        }
    }
}
