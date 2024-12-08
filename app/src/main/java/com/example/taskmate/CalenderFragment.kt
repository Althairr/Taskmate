package com.example.taskmate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CalenderFragment : Fragment() {
    private lateinit var taskAdapter: CalendarTaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var calendarView: CalendarView
    private lateinit var emptyMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calender, container, false)

        // Initialize RecyclerView and Adapter
        recyclerView = view.findViewById(R.id.calenderTaskRecyclerView)
        taskAdapter = CalendarTaskAdapter(emptyList())
        recyclerView.adapter = taskAdapter

        // Initialize Empty Message TextView
        emptyMessage = view.findViewById(R.id.emptyMessage)
        emptyMessage.visibility = View.GONE // Initially hide empty message

        // Set LayoutManager for RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize CalendarView
        calendarView = view.findViewById(R.id.calendarView)

        // Set OnDateChangeListener to get selected date
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            // Format selected date as "d MMMM yyyy"
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
            val selectedDate = dateFormat.format(selectedCalendar.time).trim()

            Log.d("CalendarFragment", "Selected Date: $selectedDate")

            // Fetch tasks for the selected date
            fetchTasksForSelectedDate(selectedDate)
        }

        return view
    }

    private fun fetchTasksForSelectedDate(selectedDate: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "Silakan masuk untuk melihat tugas", Toast.LENGTH_LONG).show()
            return
        }

        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        val tasksRef = db.collection("tasks")
        tasksRef.whereEqualTo("userId", userId)
            .whereEqualTo("deadlineDate", selectedDate.trim())
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    val groupedTasks = mutableListOf<Pair<String, List<CalenderTask>>>()  // List to hold grouped data

                    if (snapshot != null && !snapshot.isEmpty) {
                        // Group tasks by category
                        val taskMap = snapshot.documents
                            .map { it.toObject(CalenderTask::class.java)!! }
                            .groupBy { it.category }

                        // Create a list of pairs (category, tasks for that category)
                        taskMap.forEach { (category, taskList) ->
                            groupedTasks.add(Pair(category, taskList))  // Add category and task list as pair
                        }

                        // Update the adapter with the grouped tasks
                        recyclerView.visibility = View.VISIBLE
                        emptyMessage.visibility = View.GONE
                        taskAdapter.updateTasks(groupedTasks)
                    } else {
                        recyclerView.visibility = View.GONE
                        emptyMessage.visibility = View.VISIBLE
                    }
                } else {
                    recyclerView.visibility = View.GONE
                    emptyMessage.visibility = View.VISIBLE
                }
            }
    }

}