package com.example.ictmobile.models

import com.google.firebase.Timestamp
import java.util.Date

data class Payment(
    val id: String = "",
    val orderId: String = "",
    val amount: Double = 0.0,
    val status: String = "pending", // "pending", "completed", "failed"
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val paidAt: Date? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): Payment {
            val amountValue = map["amount"]
            val amount = when (amountValue) {
                is Double -> amountValue
                is Long -> amountValue.toDouble()
                is Number -> amountValue.toDouble()
                else -> 0.0
            }
            
            val paidAtTimestamp = map["paid_at"] as? Timestamp
            
            return Payment(
                id = map["id"] as? String ?: "",
                orderId = map["order_id"] as? String ?: "",
                amount = amount,
                status = map["status"] as? String ?: "pending",
                paymentMethod = map["payment_method"] as? String,
                transactionId = map["transaction_id"] as? String,
                paidAt = paidAtTimestamp?.toDate()
            )
        }
        
        fun toMap(payment: Payment): Map<String, Any> {
            val map = mutableMapOf<String, Any>(
                "order_id" to payment.orderId,
                "amount" to payment.amount,
                "status" to payment.status
            )
            
            payment.paymentMethod?.let { map["payment_method"] = it }
            payment.transactionId?.let { map["transaction_id"] = it }
            payment.paidAt?.let { map["paid_at"] = com.google.firebase.Timestamp(it.time / 1000, ((it.time % 1000) * 1000000).toInt()) }
            
            return map
        }
    }
}
