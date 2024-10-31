package com.example.taskmate

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.taskmate.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: GroupedTaskAdapter
    private val groupedItems = mutableListOf<ListItem>()
    private lateinit var currentTimeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000) // Update every second
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        currentTimeTextView = view.findViewById(R.id.current_time1)
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler.post(updateTimeRunnable)

        // Set layout manager for RecyclerView
        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter with an empty list
        adapter = GroupedTaskAdapter(groupedItems)
        binding.taskRecyclerView.adapter = adapter

        // Fetch tasks from Firebase
        fetchTasksFromFirebase()
    }

    private fun fetchTasksFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("HomeFragment", "User ID: $userId")

        if (userId == null) {
            Log.e("HomeFragment", "User not authenticated")
            return
        }

        val db = FirebaseFirestore.getInstance().collection("tasks")
            .whereEqualTo("userId", userId)

        db.addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                Log.e("HomeFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                Log.d("HomeFragment", "Documents count: ${querySnapshot.size()}")

                // Clear previous items and update the groupedItems list
                groupedItems.clear()
                val taskMap = mutableMapOf<String, MutableList<TaskItem>>()

                for (taskDocument in querySnapshot.documents) {
                    val deadlineAndTime = taskDocument.getString("deadlineAndTime")
                    val taskName = taskDocument.getString("taskName")

                    if (deadlineAndTime != null && taskName != null) {
                        val (time, date) = splitDate(deadlineAndTime)
                        val taskItem = TaskItem(taskName, time)

                        if (taskMap[date] == null) {
                            taskMap[date] = mutableListOf()
                        }
                        taskMap[date]?.add(taskItem)
                    }
                }

                for ((date, tasks) in taskMap) {
                    groupedItems.add(DateHeader(date))
                    groupedItems.addAll(tasks)
                }

                // Notify the adapter of data changes
                adapter.notifyDataSetChanged()

                if (groupedItems.isEmpty()) {
                    binding.emptyTaskMessage.visibility = View.VISIBLE
                    binding.taskRecyclerView.visibility = View.GONE
                    Log.e("HomeFragment", "No tasks available")
                } else {
                    binding.emptyTaskMessage.visibility = View.GONE
                    binding.taskRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val currentTime = DateFormat.format("HH:mm", calendar).toString()
        binding.currentTime1.text = currentTime

        // TESTING PUSH
    }


    private fun splitDate(deadlineAndTime: String): Pair<String, String> {
        val parts = deadlineAndTime.split(", ")
        return if (parts.size == 2) {
            Pair(parts[0], parts[1]) // parts[0] adalah waktu, parts[1] adalah tanggal
        } else {
            Pair("", "") // Jika format tidak valid, kembalikan pasangan kosong
        }
    }
}
