package com.example.ictmobile.models

import com.google.firebase.Timestamp
import java.util.Date

data class Order(
    val id: String = "",
    val userId: String = "",
    val machineId: String = "",
    val machineName: String = "",
    val temperature: String = "", // "cold", "warm", "hot"
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val status: String = "pending", // "pending", "active", "completed", "cancelled"
    val totalAmount: Double = 0.0,
    val paymentId: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date? = null
) {
    val progress: String
        get() {
            if (status == "completed") return "Completed"
            if (status == "cancelled") return "Cancelled"
            if (status == "pending") return "Pending"
            
            val now = Date()
            if (now < startTime) return "Pending"
            if (now >= endTime) return "Completed"
            
            val totalDuration = (endTime.time - startTime.time) / (1000 * 60) // minutes
            val elapsed = (now.time - startTime.time) / (1000 * 60) // minutes
            val progressPercent = (elapsed.toDouble() / totalDuration.toDouble()) * 100
            
            return when {
                progressPercent < 30 -> "Washing"
                progressPercent < 60 -> "Rinsing"
                progressPercent < 90 -> "Drying"
                else -> "Finalizing"
            }
        }
    
    val timeRemaining: Int?
        get() {
            if (status != "active") return null
            val now = Date()
            if (now >= endTime) return 0
            return ((endTime.time - now.time) / (1000 * 60)).toInt() // minutes
        }
    
    companion object {
        fun fromMap(map: Map<String, Any?>): Order {
            val startTimeTimestamp = map["start_time"] as? Timestamp
            val endTimeTimestamp = map["end_time"] as? Timestamp
            val createdAtTimestamp = map["created_at"] as? Timestamp
            val updatedAtTimestamp = map["updated_at"] as? Timestamp
            
            val totalAmountValue = map["total_amount"]
            val totalAmount = when (totalAmountValue) {
                is Double -> totalAmountValue
                is Long -> totalAmountValue.toDouble()
                is Number -> totalAmountValue.toDouble()
                else -> 0.0
            }
            
            // Extract payment ID - check both direct field and nested payment map
            val paymentIdFromField = map["payment_id"] as? String ?: ""
            val paymentIdFromMap = (map["payment"] as? Map<*, *>)?.get("id") as? String ?: ""
            val paymentId = if (paymentIdFromField.isNotEmpty()) paymentIdFromField else paymentIdFromMap
            
            // Extract machine name - check both direct field and nested machine map
            val machineNameFromField = map["machine_name"] as? String ?: ""
            val machineNameFromMap = (map["machine"] as? Map<*, *>)?.get("machine_name") as? String ?: ""
            val machineName = if (machineNameFromField.isNotEmpty()) machineNameFromField else machineNameFromMap
            
            return Order(
                id = map["id"] as? String ?: "",
                userId = map["user_id"] as? String ?: "",
                machineId = map["machine_id"] as? String ?: "",
                machineName = machineName,
                temperature = map["temperature"] as? String ?: "",
                startTime = startTimeTimestamp?.toDate() ?: Date(),
                endTime = endTimeTimestamp?.toDate() ?: Date(),
                status = map["status"] as? String ?: "pending",
                totalAmount = totalAmount,
                paymentId = paymentId,
                createdAt = createdAtTimestamp?.toDate() ?: Date(),
                updatedAt = updatedAtTimestamp?.toDate()
            )
        }
        
        fun toMap(order: Order): Map<String, Any> {
            val map = mutableMapOf<String, Any>(
                "user_id" to order.userId,
                "machine_id" to order.machineId,
                "temperature" to order.temperature,
                "start_time" to com.google.firebase.Timestamp(order.startTime.time / 1000, ((order.startTime.time % 1000) * 1000000).toInt()),
                "end_time" to com.google.firebase.Timestamp(order.endTime.time / 1000, ((order.endTime.time % 1000) * 1000000).toInt()),
                "status" to order.status,
                "total_amount" to order.totalAmount,
                "created_at" to com.google.firebase.Timestamp(order.createdAt.time / 1000, ((order.createdAt.time % 1000) * 1000000).toInt())
            )
            
            if (order.updatedAt != null) {
                map["updated_at"] = com.google.firebase.Timestamp(order.updatedAt.time / 1000, ((order.updatedAt.time % 1000) * 1000000).toInt())
            }
            
            return map
        }
    }
}
