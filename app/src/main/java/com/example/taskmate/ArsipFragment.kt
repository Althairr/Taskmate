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
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.taskmate.databinding.FragmentArsipBinding
import com.example.taskmate.kategori.KategoriActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.card.MaterialCardView

class ArsipFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var categoryButtonLayout: LinearLayout
    private lateinit var taskContainer: LinearLayout
    private lateinit var emptyTaskMessage: TextView
    private lateinit var currentUserId: String

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var binding: FragmentArsipBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentArsipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        categoryButtonLayout = view.findViewById(R.id.categoryButtonLayout)
        taskContainer = view.findViewById(R.id.taskContainer)
        emptyTaskMessage = view.findViewById(R.id.emptyTaskMessage)

        // Get current user ID from FirebaseAuth
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Display user profile data
        displayUserProfile()

        // Fetch categories and dynamically add buttons
        fetchCategories()

        // Handle edit categories button click
        binding.editCategoriesButton.setOnClickListener {
            val intent = Intent(requireContext(), KategoriActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-fetch user profile when the fragment becomes visible
        displayUserProfile()
    }

    private fun displayUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Default greeting and image
            binding.greetingText.text = "Halo, User"
            binding.imageViewProfile.findViewById<ImageView>(R.id.profileImageView)
                .setImageResource(R.drawable.fikri) // Default image
            return
        }

        // Fetch user data from Firestore
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    val profileImageUrl = document.getString("profileImageUrl")

                    // Update greeting text
                    binding.greetingText.text = "Halo, $username"

                    // Load profile image using Glide
                    profileImageUrl?.let {
                        loadProfileImage(it)
                    } ?: run {
                        // Use default image if no profileImageUrl is found
                        binding.imageViewProfile.findViewById<ImageView>(R.id.profileImageView)
                            .setImageResource(R.drawable.fikri)
                    }
                } else {
                    setDefaultProfile()
                }
            }
            .addOnFailureListener {
                setDefaultProfile()
                Toast.makeText(requireContext(), "Gagal memuat data pengguna.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage(imageUrl: String) {
        val profileImageView = binding.imageViewProfile.findViewById<ImageView>(R.id.profileImageView)
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.fikri) // Placeholder for loading state
            .into(profileImageView)
    }

    private fun setDefaultProfile() {
        binding.greetingText.text = "Halo, User"
        binding.imageViewProfile.findViewById<ImageView>(R.id.profileImageView)
            .setImageResource(R.drawable.fikri) // Default image
    }

    private fun fetchCategories() {
        db.collection("categories")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val categories = snapshots.documents.mapNotNull { it.getString("name") }.toMutableList()

                // Ensure a default "All" category exists
                if (!categories.contains("All")) categories.add(0, "All")

                // Clear existing category buttons
                categoryButtonLayout.removeAllViews()

                // Add category buttons
                categories.forEach { category ->
                    addCategoryButton(category)
                }

                // Initially show tasks for "All" categories
                filterTasksByCategory(null)
            }
    }

    private fun addCategoryButton(category: String) {
        val button = Button(requireContext()).apply {
            text = category
            setOnClickListener {
                filterTasksByCategory(if (category == "All") null else category)
            }
        }
        categoryButtonLayout.addView(button)
    }

    private fun filterTasksByCategory(category: String?) {
        val query = if (category == null) {
            db.collection("tasks").whereEqualTo("userId", currentUserId)
        } else {
            db.collection("tasks")
                .whereEqualTo("category", category)
                .whereEqualTo("userId", currentUserId)
        }

        query.get().addOnSuccessListener { documents ->
            if (!isAdded) return@addOnSuccessListener // Ensure the fragment is attached

            taskContainer.removeAllViews()

            if (documents.isEmpty) {
                emptyTaskMessage.visibility = View.VISIBLE
            } else {
                emptyTaskMessage.visibility = View.GONE
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
    }

    private fun createTaskView(taskName: String, category: String, deadline: String): View {
        val safeContext = context ?: return View(requireActivity()) // Ensure a valid context

        val cardView = MaterialCardView(safeContext).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
            radius = 16f
            cardElevation = 8f
        }

        val taskLayout = LinearLayout(safeContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val taskNameView = TextView(safeContext).apply {
            text = "Task: $taskName"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        val categoryView = TextView(safeContext).apply {
            text = "Category: $category"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        val deadlineView = TextView(safeContext).apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        taskContainer.removeAllViews() // Clear views
    }
}
