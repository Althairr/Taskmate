package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.example.taskmate.databinding.FragmentArsipBinding
import com.example.taskmate.kategori.KategoriActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class ArsipFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: FragmentArsipBinding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var currentUserId: String
    private var categoryListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentArsipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start time updates
        handler.post(updateTimeRunnable)

        currentUserId = auth.currentUser?.uid ?: ""

        displayUserProfile()
        fetchCategories()

        binding.editCategoriesButton.setOnClickListener {
            val intent = Intent(requireContext(), KategoriActivity::class.java)
            startActivity(intent)
        }
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val currentTime = DateFormat.format("HH:mm", calendar).toString()
        binding.currentTime.text = currentTime
    }

    override fun onResume() {
        super.onResume()
        displayUserProfile()
    }

    private fun displayUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            setDefaultProfile()
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (isAdded && document.exists()) {
                    val username = document.getString("username") ?: "User"
                    val profileImageUrl = document.getString("profileImageUrl")

                    binding.greetingText.text = "Halo, $username"

                    profileImageUrl?.let { loadProfileImage(it) } ?: setDefaultProfile()
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
        if (isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.fikri)
                .into(binding.imageViewProfile.findViewById(R.id.profileImageView))
        }
    }

    private fun setDefaultProfile() {
        binding.greetingText.text = "Halo, User"
        binding.imageViewProfile.findViewById<ImageView>(R.id.profileImageView)
            .setImageResource(R.drawable.fikri)
    }

    private fun fetchCategories() {
        categoryListener = db.collection("categories")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || !isAdded) return@addSnapshotListener

                val categories = snapshots.documents.mapNotNull { it.getString("name") }.toMutableList()

                if (!categories.contains("All")) categories.add(0, "All")

                binding.categoryButtonLayout.removeAllViews()
                categories.forEach { addCategoryButton(it) }

                filterTasksByCategory(null)
            }
    }

    private fun addCategoryButton(category: String) {
        context?.let { ctx ->
            val button = Button(ctx).apply {
                text = category
                setOnClickListener {
                    filterTasksByCategory(if (category == "All") null else category)
                }
            }
            binding.categoryButtonLayout.addView(button)
        }
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
            if (!isAdded) return@addOnSuccessListener

            binding.taskContainer.removeAllViews()

            if (documents.isEmpty) {
                binding.emptyTaskMessage.visibility = View.VISIBLE
            } else {
                binding.emptyTaskMessage.visibility = View.GONE
                for (document in documents) {
                    val taskName = document.getString("taskName") ?: "Unnamed Task"
                    val categoryName = document.getString("category") ?: "No Category"
                    val deadline = document.getString("deadlineAndTime") ?: "No Deadline"

                    val taskView = createTaskView(taskName, categoryName, deadline)
                    binding.taskContainer.addView(taskView)
                }
            }
        }
    }

    private fun createTaskView(taskName: String, category: String, deadline: String): View {
        val safeContext = context ?: return View(requireActivity())

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

        taskLayout.addView(TextView(safeContext).apply {
            text = "Task: $taskName"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        })

        taskLayout.addView(TextView(safeContext).apply {
            text = "Category: $category"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        })

        taskLayout.addView(TextView(safeContext).apply {
            text = "Deadline: $deadline"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        })

        cardView.addView(taskLayout)
        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        categoryListener?.remove() // Remove Firebase listener to avoid memory leaks
        binding.taskContainer.removeAllViews()
        handler.removeCallbacks(updateTimeRunnable)
    }
}
