package com.example.taskmate

import android.content.Intent
import android.content.res.Resources
import android.graphics.Typeface
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.example.taskmate.databinding.FragmentArsipBinding
import com.example.taskmate.kategori.KategoriActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import androidx.core.content.res.ResourcesCompat

class ArsipFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: FragmentArsipBinding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var currentUserId: String
    private var categoryListener: ListenerRegistration? = null
    private var activeCategory: String? = "Semua"
    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentArsipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                .into(binding.profileImageView)
        }
    }

    private fun setDefaultProfile() {
        binding.greetingText.text = "Halo, User"
        binding.profileImageView.setImageResource(R.drawable.fikri)
    }

    private fun fetchCategories() {
        categoryListener = db.collection("categories")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || !isAdded) return@addSnapshotListener

                val categories = snapshots.documents.mapNotNull { it.getString("name") }.toMutableList()

                if (!categories.contains("Semua")) categories.add(0, "Semua")

                binding.categoryButtonLayout.removeAllViews()
                categories.forEach { addCategoryButton(it) }

                // Set default active category to "All"
                setActiveCategory("Semua")
                filterTasksByCategory(null)
            }
    }

    private fun addCategoryButton(category: String) {
        context?.let { ctx ->
            val button = Button(ctx).apply {
                text = category
                setOnClickListener {
                    setActiveCategory(category)
                    filterTasksByCategory(if (category == "Semua") null else category)
                }
                updateButtonStyle(this, category == activeCategory)

                // Add spacing and ensure proper size
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0) // Add horizontal spacing
                }
                this.layoutParams = layoutParams
            }
            binding.categoryButtonLayout.addView(button)
        }
    }

    private fun setActiveCategory(category: String) {
        activeCategory = category
        for (i in 0 until binding.categoryButtonLayout.childCount) {
            val button = binding.categoryButtonLayout.getChildAt(i) as Button
            val isActive = button.text == category
            updateButtonStyle(button, isActive)
        }
    }

    private fun updateButtonStyle(button: Button, isActive: Boolean) {
        val ctx = requireContext()
        button.setPadding(32, 16, 32, 16) // Add padding inside the button
        if (isActive) {
            button.setBackgroundResource(R.drawable.rounded_button)
            button.setTextColor(ContextCompat.getColor(ctx, R.color.cream))
        } else {
            button.setBackgroundResource(R.drawable.rounded_outline_button)
            button.setTextColor(ContextCompat.getColor(ctx, R.color.light_red))
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

            val currentTime = System.currentTimeMillis()

            val passedDeadlineTasks = mutableListOf<View>()
            val upcomingTasks = mutableListOf<View>()
            val completedTasks = mutableListOf<View>()

            for (document in documents) {
                val taskName = document.getString("taskName") ?: "Tanpa Nama"
                val categoryName = document.getString("category") ?: "Tidak dikategorikan"
                val deadlineStr = document.getString("deadlineAndTime") ?: "Tidak ada waktu tenggat"
                val status = document.getBoolean("status") ?: false

                val deadlineMillis = parseDeadline(deadlineStr)
                val isPassedDeadline = deadlineMillis != null && deadlineMillis < currentTime

                val taskView = createTaskView(taskName, categoryName, deadlineStr, isPassedDeadline)

                when {
                    status -> completedTasks.add(taskView) // Task selesai
                    isPassedDeadline -> passedDeadlineTasks.add(taskView) // Task melewati tenggat waktu
                    else -> upcomingTasks.add(taskView) // Task akan datang
                }
            }

            binding.passedDeadlineContainer.removeAllViews()
            binding.upcomingTasksContainer.removeAllViews()
            binding.completedTasksContainer.removeAllViews()

            if (passedDeadlineTasks.isNotEmpty()) {
                binding.passedDeadlineTitle.visibility = View.VISIBLE
                binding.passedDeadlineContainer.visibility = View.VISIBLE
                passedDeadlineTasks.forEach { binding.passedDeadlineContainer.addView(it) }
            } else {
                binding.passedDeadlineTitle.visibility = View.GONE
                binding.passedDeadlineContainer.visibility = View.GONE
            }

            if (upcomingTasks.isNotEmpty()) {
                binding.upcomingTasksTitle.visibility = View.VISIBLE
                binding.upcomingTasksContainer.visibility = View.VISIBLE
                upcomingTasks.forEach { binding.upcomingTasksContainer.addView(it) }
            } else {
                binding.upcomingTasksTitle.visibility = View.GONE
                binding.upcomingTasksContainer.visibility = View.GONE
            }

            if (completedTasks.isNotEmpty()) {
                binding.completedTasksTitle.visibility = View.VISIBLE
                binding.completedTasksContainer.visibility = View.VISIBLE
                completedTasks.forEach { binding.completedTasksContainer.addView(it) }
            } else {
                binding.completedTasksTitle.visibility = View.GONE
                binding.completedTasksContainer.visibility = View.GONE
            }
        }
    }

    private fun parseDeadline(deadlineStr: String): Long? {
        return try {
            val dateFormat = java.text.SimpleDateFormat("HH:mm, d MMMM yyyy", java.util.Locale.getDefault())
            dateFormat.parse(deadlineStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun createTaskView(taskName: String, category: String, deadline: String, isPassedDeadline: Boolean): View {
        val safeContext = context ?: return View(requireActivity())

        val cardView = MaterialCardView(safeContext).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16) // Outer margin for the card
            }
            radius = 50f
            cardElevation = 8f
            setCardBackgroundColor(ContextCompat.getColor(safeContext, R.color.background_color))
            setContentPadding(8.dp, 8.dp, 8.dp, 8.dp) // Padding inside the card
        }

        val constraintLayout = ConstraintLayout(safeContext).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        // Decide the color based on the group
        val textColor = if (isPassedDeadline) R.color.gray else R.color.navy
        val barColor = if (isPassedDeadline) R.color.gray else R.color.light_red

        // Fonts from res/font
        val montserratRegular = ResourcesCompat.getFont(safeContext, R.font.montserrat_regular)
        val montserratSemiBold = ResourcesCompat.getFont(safeContext, R.font.montserrat_semibold)
        val montserratBold = ResourcesCompat.getFont(safeContext, R.font.montserrat_bold)

        // Extract the time (HH:mm) and date (d MMMM yyyy) from the deadline
        val dateFormat = java.text.SimpleDateFormat("HH:mm, d MMMM yyyy", java.util.Locale.getDefault())
        val outputTimeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val outputDateFormat = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault())
        var deadlineTime = ""
        var deadlineDate = ""
        try {
            val parsedDate = dateFormat.parse(deadline)
            deadlineTime = outputTimeFormat.format(parsedDate!!)
            deadlineDate = outputDateFormat.format(parsedDate)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Deadline text (on top of category name)
        val deadlineTextView = TextView(safeContext).apply {
            id = View.generateViewId()
            text = deadlineDate
            textSize = 14f
            setTypeface(montserratBold)
            setTextColor(ContextCompat.getColor(safeContext, textColor))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = 16.dp
            }
        }

        // Red (or Gray) vertical bar
        val verticalBar = View(safeContext).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(4.dp, 40.dp).apply {
                topToBottom = deadlineTextView.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = 16.dp
                topMargin = 8.dp
            }
            setBackgroundColor(ContextCompat.getColor(safeContext, barColor))
        }

        // Category name (below deadline)
        val categoryTextView = TextView(safeContext).apply {
            id = View.generateViewId()
            text = category
            textSize = 18f
            setTypeface(montserratSemiBold)
            setTextColor(ContextCompat.getColor(safeContext, textColor))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = deadlineTextView.id
                startToEnd = verticalBar.id
                topMargin = 12.dp
                setPadding(20, 0, 0, 0)
            }
        }

        // LinearLayout for tasks
        val taskContainer = LinearLayout(safeContext).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = categoryTextView.id
                startToStart = categoryTextView.id
                topMargin = 16.dp
                setPadding(24, 0, 0, 0)
            }
        }

        // Create task layout for each task
        val taskLayout = ConstraintLayout(safeContext).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8.dp, 0, 8.dp)
            }
        }

        val radioDot = View(safeContext).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(10.dp, 10.dp).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = 8.dp
            }
            // Use radio_uncheck for gray (passed deadline) tasks, otherwise use radio_check
            background = ContextCompat.getDrawable(
                safeContext,
                if (isPassedDeadline) R.drawable.radio_uncheck else R.drawable.radio_check
            )
        }

        val taskNameTextView = TextView(safeContext).apply {
            id = View.generateViewId()
            text = taskName
            textSize = 14f
            setTypeface(montserratRegular)
            setTextColor(ContextCompat.getColor(safeContext, textColor))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = radioDot.id
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                setPadding(16, 0, 0, 0)
            }
        }

        val taskTimeTextView = TextView(safeContext).apply {
            id = View.generateViewId()
            text = deadlineTime // Display the extracted time
            textSize = 14f
            setTypeface(montserratBold)
            setTextColor(ContextCompat.getColor(safeContext, textColor))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = 8.dp
            }
        }

        // Add views to taskLayout
        taskLayout.addView(radioDot)
        taskLayout.addView(taskNameTextView)
        taskLayout.addView(taskTimeTextView)

        // Add taskLayout to taskContainer
        taskContainer.addView(taskLayout)

        // Add all elements to ConstraintLayout
        constraintLayout.addView(verticalBar)
        constraintLayout.addView(deadlineTextView)
        constraintLayout.addView(categoryTextView)
        constraintLayout.addView(taskContainer)

        // Add ConstraintLayout to the cardView
        cardView.addView(constraintLayout)

        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        categoryListener?.remove()
        binding.passedDeadlineContainer.removeAllViews()
        binding.upcomingTasksContainer.removeAllViews()
        handler.removeCallbacks(updateTimeRunnable)
    }
}