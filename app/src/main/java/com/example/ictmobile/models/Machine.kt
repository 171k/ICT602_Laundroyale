package com.example.ictmobile.models

data class Machine(
    val id: String = "",
    val machineName: String = "",
    val type: String = "", // "washer" or "dryer"
    val price: Double = 0.0,
    val status: String = "available" // "available", "maintenance", "unavailable"
) {
    val availability: String
        get() = if (status == "available") "Available" else "Unavailable"
    
    companion object {
        fun fromMap(map: Map<String, Any?>): Machine {
            val priceValue = map["price"]
            val price = when (priceValue) {
                is Double -> priceValue
                is Long -> priceValue.toDouble()
                is Number -> priceValue.toDouble()
                else -> 0.0
            }
            
            return Machine(
                id = map["id"] as? String ?: "",
                machineName = map["machine_name"] as? String ?: "",
                type = map["type"] as? String ?: "",
                price = price,
                status = map["status"] as? String ?: "available"
            )
        }
        
        fun toMap(machine: Machine): Map<String, Any> {
            return mapOf(
                "id" to machine.id,
                "machine_name" to machine.machineName,
                "type" to machine.type,
                "price" to machine.price,
                "status" to machine.status
            )
        }
    }
}
