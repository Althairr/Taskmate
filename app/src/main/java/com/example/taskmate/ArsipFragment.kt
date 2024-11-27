package com.example.taskmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class ArsipFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var categoryContainer: LinearLayout
    private lateinit var taskContainer: LinearLayout
    private lateinit var currentUserId: String
    private lateinit var currentTimeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_arsip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        categoryContainer = view.findViewById(R.id.categoryContainer)
        taskContainer = view.findViewById(R.id.taskContainer)
        currentTimeTextView = view.findViewById(R.id.current_time) // Get reference to the time TextView

        // Get current user ID from FirebaseAuth
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Fetch and dynamically add category buttons
        fetchCategories()

        // Update the current time every second
        updateCurrentTime()
    }

    private fun fetchCategories() {
        db.collection("tasks")
            .whereEqualTo("userId", currentUserId) // Match the userId
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                if (snapshots != null) {
                    // Get unique categories
                    val categories = snapshots.documents.map { it.getString("category") }.distinct()

                    // Clear existing buttons
                    categoryContainer.removeAllViews()

                    // Add "Semua" button (for All tasks) once
                    addCategoryButton("Semua")

                    // Add buttons for each category except "Semua"
                    categories.filter { it != "Semua" }.forEach { category ->
                        category?.let { addCategoryButton(it) }
                    }

                    // Show all tasks initially (when "Semua" is selected)
                    filterTasksByCategory(null)
                }
            }
    }

    private fun addCategoryButton(category: String) {
        val button = Button(requireContext()).apply {
            text = category
            setOnClickListener {
                filterTasksByCategory(if (category == "Semua") null else category)
            }
        }
        categoryContainer.addView(button)
    }

    private fun filterTasksByCategory(category: String?) {
        val query = if (category == null) {
            // "Semua" selected, show all tasks
            db.collection("tasks").whereEqualTo("userId", currentUserId)
        } else {
            // Filter tasks by the selected category
            db.collection("tasks").whereEqualTo("category", category).whereEqualTo("userId", currentUserId)
        }

        query.get().addOnSuccessListener { documents ->
            // Clear the current task list UI
            taskContainer.removeAllViews()

            for (document in documents) {
                val taskName = document.getString("taskName") ?: "Unnamed Task"
                val categoryName = document.getString("category") ?: "No Category"
                val deadline = document.getString("deadlineAndTime") ?: "No Deadline"

                // Dynamically create task views
                val taskView = createTaskView(taskName, categoryName, deadline)
                taskContainer.addView(taskView)
            }
        }
    }

    private fun createTaskView(taskName: String, category: String, deadline: String): View {
        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
            radius = 16f
            cardElevation = 8f
        }

        val taskLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val taskNameView = TextView(requireContext()).apply {
            text = "Task: $taskName"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        val categoryView = TextView(requireContext()).apply {
            text = "Category: $category"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        val deadlineView = TextView(requireContext()).apply {
            text = "Deadline: $deadline"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        taskLayout.addView(taskNameView)
        taskLayout.addView(categoryView)
        taskLayout.addView(deadlineView)
        cardView.addView(taskLayout)

        return cardView
    }

    // Update the current time every second
    private fun updateCurrentTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Update the time TextView with current time
        currentTimeTextView.text = timeFormat.format(calendar.time)

        // Update every second
        currentTimeTextView.postDelayed({
            updateCurrentTime()
        }, 1000)
    }
}
