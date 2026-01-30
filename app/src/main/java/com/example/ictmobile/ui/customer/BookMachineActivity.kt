package com.example.ictmobile.ui.customer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ictmobile.R
import com.example.ictmobile.adapters.MachineAdapter
import com.example.ictmobile.databinding.ActivityBookMachineBinding
import com.example.ictmobile.models.Machine
import com.example.ictmobile.services.FirebaseService
import java.util.*

class BookMachineActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookMachineBinding
    private val firebaseService = FirebaseService.getInstance()
    private val malaysiaTimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    private var selectedMachine: Machine? = null
    private var selectedTemperature: String = "warm" // cold, warm, hot
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private var selectedDuration: Int = 60 // minutes
    private var datePickerDialog: DatePickerDialog? = null
    private var timePickerDialog: TimePickerDialog? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookMachineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        setupRecyclerView()
        setupDurationSpinner()
        setupClickListeners()
        loadMachines()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Dismiss any open dialogs to prevent window leaks
        datePickerDialog?.dismiss()
        timePickerDialog?.dismiss()
    }
    
    override fun onPause() {
        super.onPause()
        // Dismiss dialogs when activity is paused to prevent leaks
        datePickerDialog?.dismiss()
        timePickerDialog?.dismiss()
    }
    
    private fun setupRecyclerView() {
        binding.rvMachines.layoutManager = GridLayoutManager(this, 2)
    }
    
    private fun setupDurationSpinner() {
        val durations = arrayOf("30 minutes", "1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuration.adapter = adapter
        binding.spinnerDuration.setSelection(1) // Default to 1 hour
    }
    
    private fun setupClickListeners() {
        binding.btnSelectDate.setOnClickListener { showDatePicker() }
        binding.btnSelectTime.setOnClickListener { showTimePicker() }
        binding.btnNow.setOnClickListener { setToNearestAvailableTime() }
        
        binding.rbCold.setOnClickListener { selectedTemperature = "cold"; updatePrice() }
        binding.rbWarm.setOnClickListener { selectedTemperature = "warm"; updatePrice() }
        binding.rbHot.setOnClickListener { selectedTemperature = "hot"; updatePrice() }
        
        binding.spinnerDuration.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedDuration = when (position) {
                    0 -> 30
                    1 -> 60
                    2 -> 90
                    3 -> 120
                    4 -> 150
                    5 -> 180
                    else -> 60
                }
                updatePrice()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        binding.btnBookNow.setOnClickListener { createBooking() }
    }
    
    private fun loadMachines() {
        android.util.Log.d("BookMachine", "Starting to load machines...")
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        firebaseService.getMachines(null)
            .addOnSuccessListener { machines ->
                android.util.Log.d("BookMachine", "Machines loaded successfully: ${machines.size} machines")
                binding.progressBar.visibility = android.view.View.GONE
                
                try {
                    val adapter = MachineAdapter(machines) { machine ->
                        selectedMachine = machine
                        updateSelectedMachineUI()
                        updatePrice()
                    }
                    binding.rvMachines.adapter = adapter
                    android.util.Log.d("BookMachine", "Adapter set successfully")
                } catch (e: Exception) {
                    android.util.Log.e("BookMachine", "Error setting adapter: ${e.message}", e)
                    Toast.makeText(this, "Error displaying machines: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("BookMachine", "Failed to load machines: ${exception.message}", exception)
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load machines: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun updateSelectedMachineUI() {
        selectedMachine?.let { machine ->
            // Use the same formatting logic as the adapter
            val displayName = when {
                machine.machineName.isNotEmpty() -> machine.machineName
                machine.id.isNotEmpty() -> {
                    // Format ID like "dryer_1" -> "Dryer 1" or "washer_1" -> "Washer 1"
                    machine.id.replace("_", " ").split(" ").joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { it.uppercase() }
                    }
                }
                else -> "Unknown Machine"
            }
            binding.tvSelectedMachine.text = "Selected: $displayName"
            binding.tvSelectedMachine.visibility = android.view.View.VISIBLE
            android.util.Log.d("BookMachine", "Selected machine - Name: '${machine.machineName}', ID: '${machine.id}', Display: '$displayName'")
        } ?: run {
            binding.tvSelectedMachine.visibility = android.view.View.GONE
        }
    }
    
    private fun showDatePicker() {
        // Dismiss any existing dialog first
        datePickerDialog?.dismiss()
        
        val calendar = selectedDate ?: Calendar.getInstance(malaysiaTimeZone)
        datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance(malaysiaTimeZone)
                cal.set(year, month, dayOfMonth)
                selectedDate = cal
                updateDateDisplay()
                validateDateTime()
                datePickerDialog = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            val maxDate = Calendar.getInstance(malaysiaTimeZone).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            datePicker.maxDate = maxDate.timeInMillis
            setOnCancelListener { datePickerDialog = null }
        }
        datePickerDialog?.show()
    }
    
    private fun showTimePicker() {
        // Dismiss any existing dialog first
        timePickerDialog?.dismiss()
        
        val calendar = selectedTime ?: Calendar.getInstance(malaysiaTimeZone)
        timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance(malaysiaTimeZone)
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                selectedTime = cal
                updateTimeDisplay()
                validateDateTime()
                timePickerDialog = null
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).apply {
            setOnCancelListener { timePickerDialog = null }
        }
        timePickerDialog?.show()
    }
    
    private fun updateDateDisplay() {
        selectedDate?.let { date ->
            val day = date.get(Calendar.DAY_OF_MONTH)
            val month = date.get(Calendar.MONTH) + 1
            val year = date.get(Calendar.YEAR)
            binding.btnSelectDate.text = "$day/$month/$year"
        } ?: run {
            binding.btnSelectDate.text = "Select Date"
        }
    }
    
    private fun updateTimeDisplay() {
        selectedTime?.let { time ->
            val hour = String.format("%02d", time.get(Calendar.HOUR_OF_DAY))
            val minute = String.format("%02d", time.get(Calendar.MINUTE))
            binding.btnSelectTime.text = "$hour:$minute"
        } ?: run {
            binding.btnSelectTime.text = "Select Time"
        }
    }
    
    private fun setToNearestAvailableTime() {
        val now = Calendar.getInstance(malaysiaTimeZone)
        val nearestTime = Calendar.getInstance(malaysiaTimeZone).apply {
            // Add 1 minute to current time
            add(Calendar.MINUTE, 1)
            // Round up to the next minute (clear seconds and milliseconds)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Check if the nearest time is within 24 hours
        val maxDateTime = Calendar.getInstance(malaysiaTimeZone).apply {
            add(Calendar.HOUR_OF_DAY, 24)
        }
        
        if (nearestTime.after(maxDateTime)) {
            Toast.makeText(this, "Cannot book more than 24 hours in advance", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Set date and time
        selectedDate = Calendar.getInstance(malaysiaTimeZone).apply {
            timeInMillis = nearestTime.timeInMillis
        }
        selectedTime = nearestTime
        
        // Update displays
        updateDateDisplay()
        updateTimeDisplay()
        
        // Validate
        validateDateTime()
        
        Toast.makeText(this, "Time set to nearest available slot", Toast.LENGTH_SHORT).show()
    }
    
    private fun validateDateTime() {
        if (selectedDate != null && selectedTime != null) {
            val bookingDateTime = Calendar.getInstance(malaysiaTimeZone).apply {
                timeInMillis = selectedDate!!.timeInMillis
                set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val now = Calendar.getInstance(malaysiaTimeZone).apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val maxDateTime = Calendar.getInstance(malaysiaTimeZone).apply {
                add(Calendar.HOUR_OF_DAY, 24)
            }
            
            // Allow bookings if the time is at least 1 minute in the future
            val oneMinuteFromNow = Calendar.getInstance(malaysiaTimeZone).apply {
                add(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            if (bookingDateTime.before(oneMinuteFromNow)) {
                Toast.makeText(this, "Selected time must be at least 1 minute in the future", Toast.LENGTH_SHORT).show()
                selectedTime = null
                updateTimeDisplay()
            } else if (bookingDateTime.after(maxDateTime)) {
                Toast.makeText(this, "Booking must be within 24 hours", Toast.LENGTH_SHORT).show()
                selectedDate = null
                selectedTime = null
                updateDateDisplay()
                updateTimeDisplay()
            }
        }
    }
    
    private fun updatePrice() {
        selectedMachine?.let { machine ->
            val hours = selectedDuration / 60.0
            val basePrice = when (selectedTemperature) {
                "cold" -> 4.0
                "warm" -> 5.0
                "hot" -> 6.0
                else -> 5.0
            }
            val totalPrice = basePrice * hours
            binding.tvPrice.text = "RM ${String.format("%.2f", totalPrice)}"
        } ?: run {
            binding.tvPrice.text = "RM 0.00"
        }
    }
    
    private fun createBooking() {
        if (selectedMachine == null) {
            Toast.makeText(this, "Please select a machine", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedDate == null || selectedTime == null) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Combine date and time - ensure it's in the future (using Malaysia timezone)
        val startTime = Calendar.getInstance(malaysiaTimeZone).apply {
            timeInMillis = selectedDate!!.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Final validation: ensure start time is at least 1 minute in the future
        val oneMinuteFromNow = Calendar.getInstance(malaysiaTimeZone).apply {
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (startTime.before(oneMinuteFromNow)) {
            Toast.makeText(this, "Selected time must be at least 1 minute in the future", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnBookNow.isEnabled = true
            return
        }
        
        val endTime = Calendar.getInstance(malaysiaTimeZone).apply {
            timeInMillis = startTime.timeInMillis
            add(Calendar.MINUTE, selectedDuration)
        }
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnBookNow.isEnabled = false
        
        android.util.Log.d("BookMachine", "Creating order with startTime: ${startTime.time}, endTime: ${endTime.time}")
        
        firebaseService.createOrder(
            currentUser.uid,
            selectedMachine!!.id,
            selectedTemperature,
            startTime.time,
            endTime.time
        ).addOnSuccessListener { orderId ->
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnBookNow.isEnabled = true
            android.util.Log.d("BookMachine", "Order created successfully with ID: $orderId")
            Toast.makeText(this, "Booking created successfully!", Toast.LENGTH_SHORT).show()
            
            // Dismiss any open dialogs before navigating
            datePickerDialog?.dismiss()
            timePickerDialog?.dismiss()
            
            // Navigate to payment
            val intent = android.content.Intent(this, PaymentActivity::class.java)
            intent.putExtra("order_id", orderId)
            android.util.Log.d("BookMachine", "Navigating to PaymentActivity with order_id: $orderId")
            startActivity(intent)
            finish()
        }.addOnFailureListener { exception ->
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnBookNow.isEnabled = true
            android.util.Log.e("BookMachine", "Booking failed: ${exception.message}", exception)
            Toast.makeText(this, "Booking failed: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }
}
