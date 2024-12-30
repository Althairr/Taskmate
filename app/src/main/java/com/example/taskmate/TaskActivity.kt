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

                // Pastikan "Tidak dikategorikan" selalu ada di posisi pertama
                if (!categories.contains("Tidak dikategorikan")) {
                    categories.add(0, "Tidak dikategorikan")
                } else {
                    categories.remove("Tidak dikategorikan")
                    categories.add(0, "Tidak dikategorikan")
                }

                // Pastikan "Add Category" selalu ada di posisi terakhir
                if (!categories.contains("Tambahkan Kategori")) {
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

        // Set "Tidak dikategorikan" as the default selection if it exists in the list
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

        builder.setPositiveButton("Tambah") { _, _ ->
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
            "userId" to userId // Tambahkan userId ke data kategori
        )

        firestore.collection("categories")
            .add(categoryData)
            .addOnSuccessListener {
                categories.add(categories.size - 1, categoryName) // Tambahkan sebelum "Tambahkan Kategori"
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error adding document: ", exception)
                Toast.makeText(this, "Gagal menambahkan kategori", Toast.LENGTH_SHORT).show()
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
            val deadlineDate = String.format(
                "%d %s %d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                calendar.get(Calendar.YEAR)
            )

            val taskData = mapOf(
                "userId" to userId,
                "category" to category,
                "taskName" to taskName,
                "deadlineAndTime" to deadlineAndTime,
                "time" to time,
                "status" to false, // Tugas baru belum selesai
                "deadlineDate" to deadlineDate // Menyimpan hanya tanggal
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
