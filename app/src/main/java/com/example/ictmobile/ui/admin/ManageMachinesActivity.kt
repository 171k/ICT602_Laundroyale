package com.example.ictmobile.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.ArrayAdapter
import com.example.ictmobile.adapters.MachineAdapter
import com.example.ictmobile.databinding.ActivityManageMachinesBinding
import com.example.ictmobile.models.Machine
import com.example.ictmobile.services.FirebaseService

class ManageMachinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageMachinesBinding
    private val firebaseService = FirebaseService.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageMachinesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.rvMachines.layoutManager = GridLayoutManager(this, 2)
        setupClickListeners()
        loadMachines()
    }
    
    private fun setupClickListeners() {
        binding.btnAddMachine.setOnClickListener { showAddMachineDialog() }
    }
    
    private fun loadMachines() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        firebaseService.getMachines(null)
            .addOnSuccessListener { machines ->
                binding.progressBar.visibility = android.view.View.GONE
                if (machines.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.rvMachines.visibility = android.view.View.GONE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                    binding.rvMachines.visibility = android.view.View.VISIBLE
                    val adapter = MachineAdapter(machines) { machine ->
                        showMachineOptions(machine)
                    }
                    binding.rvMachines.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to load machines: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showMachineOptions(machine: Machine) {
        AlertDialog.Builder(this)
            .setTitle(machine.machineName)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditMachineDialog(machine)
                    1 -> deleteMachine(machine)
                }
            }
            .show()
    }
    
    private fun showAddMachineDialog() {
        val dialogView = layoutInflater.inflate(com.example.ictmobile.R.layout.dialog_add_machine, null)
        val etName = dialogView.findViewById<android.widget.EditText>(com.example.ictmobile.R.id.etMachineName)
        val spinnerType = dialogView.findViewById<android.widget.Spinner>(com.example.ictmobile.R.id.spinnerMachineType)
        val etPrice = dialogView.findViewById<android.widget.EditText>(com.example.ictmobile.R.id.etMachinePrice)
        
        // Setup type spinner
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Washer", "Dryer"))
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
        
        AlertDialog.Builder(this)
            .setTitle("Add Machine")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val type = spinnerType.selectedItem.toString().lowercase()
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                
                if (name.isEmpty() || price <= 0) {
                    Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val machine = Machine("", name, type, price, "available")
                binding.progressBar.visibility = android.view.View.VISIBLE
                firebaseService.createMachine(machine)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Machine added successfully", Toast.LENGTH_SHORT).show()
                        loadMachines()
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Failed to add machine: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditMachineDialog(machine: Machine) {
        val dialogView = layoutInflater.inflate(com.example.ictmobile.R.layout.dialog_add_machine, null)
        val etName = dialogView.findViewById<android.widget.EditText>(com.example.ictmobile.R.id.etMachineName)
        val spinnerType = dialogView.findViewById<android.widget.Spinner>(com.example.ictmobile.R.id.spinnerMachineType)
        val etPrice = dialogView.findViewById<android.widget.EditText>(com.example.ictmobile.R.id.etMachinePrice)
        val spinnerStatus = dialogView.findViewById<android.widget.Spinner>(com.example.ictmobile.R.id.spinnerMachineStatus)
        
        // Setup spinners
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Washer", "Dryer"))
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
        spinnerType.setSelection(if (machine.type == "washer") 0 else 1)
        
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Available", "Maintenance", "Unavailable"))
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        spinnerStatus.setSelection(when (machine.status) {
            "available" -> 0
            "maintenance" -> 1
            else -> 2
        })
        
        etName.setText(machine.machineName)
        etPrice.setText(machine.price.toString())
        
        AlertDialog.Builder(this)
            .setTitle("Edit Machine")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val type = spinnerType.selectedItem.toString().lowercase()
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val status = spinnerStatus.selectedItem.toString().lowercase()
                
                if (name.isEmpty() || price <= 0) {
                    Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val updatedMachine = machine.copy(machineName = name, type = type, price = price, status = status)
                binding.progressBar.visibility = android.view.View.VISIBLE
                firebaseService.updateMachine(machine.id, updatedMachine)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Machine updated successfully", Toast.LENGTH_SHORT).show()
                        loadMachines()
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Failed to update machine: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteMachine(machine: Machine) {
        AlertDialog.Builder(this)
            .setTitle("Delete Machine")
            .setMessage("Are you sure you want to delete ${machine.machineName}?")
            .setPositiveButton("Delete") { _, _ ->
                binding.progressBar.visibility = android.view.View.VISIBLE
                firebaseService.deleteMachine(machine.id)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Machine deleted successfully", Toast.LENGTH_SHORT).show()
                        loadMachines()
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Failed to delete machine: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
