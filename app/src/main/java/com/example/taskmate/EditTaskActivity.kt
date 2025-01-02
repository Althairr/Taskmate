package com.example.taskmate

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
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

class EditTaskActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var taskNameInput: EditText
    private lateinit var deadlineAndTimeInput: TextView
    private lateinit var submitButton: TextView
    private lateinit var deleteButton: TextView
    private val calendar = Calendar.getInstance()
    private lateinit var firestore: FirebaseFirestore
    private val categories = mutableListOf<String>()
    private var taskId: String? = null // ID task untuk edit dan delete

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_edit_task)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        categorySpinner = findViewById(R.id.categorySpinner)
        taskNameInput = findViewById(R.id.taskNameInput)
        deadlineAndTimeInput = findViewById(R.id.deadlineAndTimeInput)
        submitButton = findViewById(R.id.updateText)
        deleteButton = findViewById(R.id.deleteText)
        timeSpinner = findViewById(R.id.notificationTimeSpinner)
        deadlineAndTimeInput.setOnClickListener { openDatePicker() }

        submitButton.setOnClickListener { handleUpdate() }
        deleteButton.setOnClickListener { handleDelete() }

        // Get task ID from Intent
        taskId = intent.getStringExtra("taskId")
        if (taskId != null) {
            loadTaskData(taskId!!)
        }

        // Setup listeners for submit and delete buttons
        submitButton.setOnClickListener { handleUpdate() }
        deleteButton.setOnClickListener { handleDelete() }

        // Load categories from Firestore
        loadCategoriesFromFirestore()

        val cancelButton = findViewById<TextView>(R.id.cancelButton)

        cancelButton.setOnClickListener {
            // Navigate explicitly to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Optionally, finish EditTaskActivity if you don't want the user to come back to it
            finish()  // Close the current activity after navigating
        }
    }

    private fun loadCategoriesFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak ditemukan. Harap login terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("categories")
            .whereEqualTo("userId", userId) // Filter berdasarkan userId
            .get()
            .addOnSuccessListener { documents ->
                categories.clear()
                for (document in documents) {
                    document.getString("name")?.let { categories.add(it) }
                }

                if (!categories.contains("Tidak dikategorikan")) {
                    categories.add(0, "Tidak dikategorikan")
                }

                if (!categories.contains("Tambahkan kategori")) {
                    categories.add("Tambahkan Kategori")
                }

                setupCategorySpinner()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
                categories.clear()
                categories.add("Tidak dikategorikan")
                categories.add("Tambahkan Kategori")
                setupCategorySpinner()
            }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        val defaultIndex = categories.indexOf("Tidak dikategorikan")
        if (defaultIndex != -1) {
            categorySpinner.setSelection(defaultIndex)
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                if (categories[position] == "Tambahkan Kategori") {
                    showAddCategoryDialog(adapter)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showAddCategoryDialog(adapter: ArrayAdapter<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tambahkan Kategori")

        val input = EditText(this)
        input.hint = "Masukkan nama kategori"
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val newCategory = input.text.toString().trim()
            if (newCategory.isNotEmpty()) {
                addCategoryToFirestore(newCategory, adapter)
            }
        }
        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun addCategoryToFirestore(categoryName: String, adapter: ArrayAdapter<String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak ditemukan. Harap login terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryData = hashMapOf(
            "name" to categoryName,
            "userId" to userId
        )

        firestore.collection("categories")
            .add(categoryData)
            .addOnSuccessListener {
                categories.add(categories.size - 1, categoryName) // Tambahkan sebelum "Add Category"
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error adding document: ", exception)
                Toast.makeText(this, "Gagal menambahkan kategori", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTaskData(taskId: String) {
        firestore.collection("tasks").document(taskId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val taskName = document.getString("taskName") ?: ""
                    val deadlineAndTime = document.getString("deadlineAndTime") ?: ""
                    val category = document.getString("category") ?: "Tidak dikategorikan"
                    val time = document.getString("time") ?: ""

                    taskNameInput.setText(taskName)
                    deadlineAndTimeInput.text = deadlineAndTime

                    val categoryPosition = categories.indexOf(category)
                    if (categoryPosition != -1) {
                        categorySpinner.setSelection(categoryPosition)
                    }

                    // Safely cast the adapter to ArrayAdapter<String>
                    val adapter = timeSpinner.adapter as? ArrayAdapter<String>

                    // Check if the adapter is not null before calling getPosition
                    if (adapter != null) {
                        val timePosition = adapter.getPosition(time)
                        timeSpinner.setSelection(timePosition)
                    } else {
                        Log.e("EditTaskActivity", "Failed to cast the spinner adapter to ArrayAdapter<String>")
                    }

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat data task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleUpdate() {
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
                "time" to time,
                "status" to false // Tugas baru belum selesai
            )

            if (taskId != null) {
                firestore.collection("tasks").document(taskId!!)
                    .set(taskData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal memperbarui tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun handleDelete() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Tugas")
            .setMessage("Apakah Anda yakin ingin menghapus tugas ini?")
            .setPositiveButton("Ya") { _, _ ->
                if (taskId != null) {
                    firestore.collection("tasks").document(taskId!!)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Tugas berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menghapus tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
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
            .build()

        picker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)
            val formattedDate = String.format(
                "%04d-%02d-%02d %02d:%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
            deadlineAndTimeInput.text = formattedDate
        }

        picker.show(supportFragmentManager, picker.toString())
    }
}
