package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.taskmate.kategori.KategoriActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        return inflater.inflate(R.layout.fragment_arsip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryContainer = view.findViewById(R.id.categoryContainer)
        taskContainer = view.findViewById(R.id.taskContainer)
        currentTimeTextView = view.findViewById(R.id.current_time)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Fetch and dynamically add categories and tasks
        fetchCategories()

        // Update the current time every second
        updateCurrentTime()
        val hamburgerIcon = view.findViewById<ImageView>(R.id.hamburger_menu)
        hamburgerIcon.setOnClickListener {
            val intent = Intent(requireContext(), KategoriActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchCategories() {
        db.collection("tasks")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                if (snapshots != null) {
                    val categories = snapshots.documents.mapNotNull { it.getString("category") }.distinct()

                    // Clear the category container and add "All Tasks" button
                    categoryContainer.removeAllViews()
                    addCategoryButton("All Tasks") // Show all tasks including "Tidak dikategorikan"

                    // Add buttons for each distinct category
                    categories.filter { it != "Tidak dikategorikan" }.forEach { category ->
                        addCategoryButton(category)
                    }

                    // Initially show all tasks
                    filterTasksByCategory(null)
                }
            }
    }



    private fun addCategoryButton(category: String) {
        val button = Button(requireContext()).apply {
            text = category
            setOnClickListener {
                filterTasksByCategory(if (category == "All Tasks") null else category)
            }
        }
        categoryContainer.addView(button)
    }

    private fun filterTasksByCategory(category: String?) {
        val query = if (category == null) {
            db.collection("tasks").whereEqualTo("userId", currentUserId)
        } else {
            db.collection("tasks")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("category", category)
        }

        query.get().addOnSuccessListener { documents ->
            taskContainer.removeAllViews()
            if (documents.isEmpty) {
                val emptyView = TextView(requireContext()).apply {
                    text = "No tasks available."
                    textSize = 16f
                }
                taskContainer.addView(emptyView)
            } else {
                documents.forEach { document ->
                    val taskName = document.getString("taskName") ?: "Unnamed Task"
                    val categoryName = document.getString("category") ?: "No Category"
                    val deadline = document.getString("deadlineAndTime") ?: "No Deadline"
                    val taskView = createTaskView(taskName, categoryName, deadline)
                    taskContainer.addView(taskView)
                }
            }
        }
    }

    private fun createTaskView(taskName: String, category: String, deadline: String): View {
        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 16, 16, 16) }
            radius = 16f
            cardElevation = 8f
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        layout.addView(TextView(requireContext()).apply {
            text = "Task: $taskName"
            textSize = 16f
        })

        layout.addView(TextView(requireContext()).apply {
            text = "Category: $category"
            textSize = 14f
        })

        layout.addView(TextView(requireContext()).apply {
            text = "Deadline: $deadline"
            textSize = 14f
        })

        cardView.addView(layout)
        return cardView
    }

    private fun updateCurrentTime() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTimeTextView.text = timeFormat.format(Date())
        currentTimeTextView.postDelayed({ updateCurrentTime() }, 1000)
    }
}
