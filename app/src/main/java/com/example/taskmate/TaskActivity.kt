package com.example.taskmate

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
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
    // Firestore reference
    private lateinit var firestore: FirebaseFirestore

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

        // Set OnClickListener to return to MainActivity
        returnToMainMenu.setOnClickListener {
            // Finish the current activity, which will go back to MainActivity
            finish()
        }
    }


    private fun openDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePicker,
            { _, year, month, dayOfMonth ->
                // Set the selected date in the calendar
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // After selecting the date, open the time picker
                openTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun openTimePicker() {
        // Create the Material Time Picker
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)  // Use 24-hour format
            .setHour(calendar.get(Calendar.HOUR_OF_DAY)) // Set the default hour
            .setMinute(calendar.get(Calendar.MINUTE)) // Set the default minute
            .setTitleText("Pilih Waktu Notifikasi") // Title of the picker
            .setTheme(R.style.CustomMaterialTimePicker) // Apply custom style
            .build()

        // Show the picker
        picker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")

        // Handle the user selection
        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute

            // Set the selected time in the calendar
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            // Format the date and time together
            val formattedDateTime = String.format(
                "%02d:%02d, %d %s %d",
                selectedHour,
                selectedMinute,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                calendar.get(Calendar.YEAR)
            )

            // Display the combined date and time in deadlineAndTimeInput
            deadlineAndTimeInput.text = formattedDateTime
        }
    }

    private fun handleSubmit() {
        val category = categorySpinner.selectedItem.toString()
        val taskName = taskNameInput.text.toString()
        val deadlineAndTime = deadlineAndTimeInput.text.toString()
        val time = timeSpinner.selectedItem.toString()

        // Ambil user ID dari Firebase Auth
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (taskName.isEmpty() || deadlineAndTime == "Pilih tanggal dan waktu") {
            Toast.makeText(this, "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
        } else {
            // Create a map of data to store
            val taskData = mapOf(
                "userId" to userId, // Tambahkan userId
                "category" to category,
                "taskName" to taskName,
                "deadlineAndTime" to deadlineAndTime,
                "time" to time
            )

            // Store the data in Firestore
            firestore.collection("tasks")
                .add(taskData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Tugas berhasil ditambahkan ke Firestore!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menambahkan tugas ke Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}