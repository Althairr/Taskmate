package com.example.taskmate

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TaskActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var taskNameInput: EditText
    private lateinit var deadlineAndTimeInput: TextView
    private lateinit var submitButton: Button
    private val calendar = Calendar.getInstance()
    private lateinit var firestore: FirebaseFirestore
    private val categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        categorySpinner = findViewById(R.id.categorySpinner)
        taskNameInput = findViewById(R.id.taskNameInput)
        deadlineAndTimeInput = findViewById(R.id.deadlineAndTimeInput)
        submitButton = findViewById(R.id.submitButton)
        timeSpinner = findViewById(R.id.notificationTimeSpinner)
        deadlineAndTimeInput.setOnClickListener { openDatePicker() }

        submitButton.setOnClickListener { handleSubmit() }

        val returnToMainMenu = findViewById<TextView>(R.id.cancelButton)
        returnToMainMenu.setOnClickListener { finish() }

        // Load categories from Firestore
        loadCategoriesFromFirestore()
    }

    private fun loadCategoriesFromFirestore() {
        firestore.collection("categories").get()
            .addOnSuccessListener { documents ->
                categories.clear()
                for (document in documents) {
                    document.getString("name")?.let { categories.add(it) }
                }

                // Ensure "Semua" is always at the top
                if (!categories.contains("Semua")) {
                    categories.add(0, "Semua")
                } else {
                    categories.remove("Semua")
                    categories.add(0, "Semua")
                }

                // Ensure "Add Category" is always at the end
                if (!categories.contains("Add Category")) {
                    categories.add("Add Category")
                }
                setupCategorySpinner()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
                categories.clear()
                categories.add("Semua")
                categories.add("Add Category")
                setupCategorySpinner()
            }
    }


    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Set "Semua" as the default selection if it exists in the list
        val defaultIndex = categories.indexOf("Semua")
        if (defaultIndex != -1) {
            categorySpinner.setSelection(defaultIndex)
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                if (categories[position] == "Add Category") {
                    showAddCategoryDialog(adapter)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showAddCategoryDialog(adapter: ArrayAdapter<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Category")

        val input = EditText(this)
        input.hint = "Enter category name"
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val newCategory = input.text.toString().trim()
            if (newCategory.isNotEmpty()) {
                addCategoryToFirestore(newCategory, adapter)
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val defaultIndex = categories.indexOf("Semua")
        if (defaultIndex != -1) {
            categorySpinner.setSelection(defaultIndex)
        }
        builder.show()
    }

    private fun addCategoryToFirestore(categoryName: String, adapter: ArrayAdapter<String>) {
        val categoryData = hashMapOf("name" to categoryName)
        firestore.collection("categories")
            .add(categoryData)
            .addOnSuccessListener {
                categories.add(categories.size - 1, categoryName) // Add before "Add Category"
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error adding document: ", exception)
                Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePicker,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                openTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    @SuppressLint("DefaultLocale")
    private fun openTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Pilih Waktu Notifikasi")
            .setTheme(R.style.CustomMaterialTimePicker)
            .build()

        picker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")

        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            val formattedDateTime = String.format(
                "%02d:%02d, %d %s %d",
                selectedHour,
                selectedMinute,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                calendar.get(Calendar.YEAR)
            )
            deadlineAndTimeInput.text = formattedDateTime
        }
    }

    private fun handleSubmit() {
        val category = categorySpinner.selectedItem.toString()
        val taskName = taskNameInput.text.toString()
        val deadlineAndTime = deadlineAndTimeInput.text.toString()
        val time = timeSpinner.selectedItem.toString()

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (taskName.isEmpty() || deadlineAndTime == "Pilih tanggal dan waktu") {
            Toast.makeText(this, "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
        } else {
            val taskData = mapOf(
                "userId" to userId,
                "category" to category,
                "taskName" to taskName,
                "deadlineAndTime" to deadlineAndTime,
                "time" to time
            )

            firestore.collection("tasks")
                .add(taskData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Tugas berhasil ditambahkan ke Firestore!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menambahkan tugas ke Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
