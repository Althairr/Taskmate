package com.example.taskmate

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupedTaskAdapter(private val items: List<ListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                // Inflating the DateHeader layout
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            1 -> {
                // Inflating the TaskItem layout
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view)
            }
            2 -> {
                // Inflating the CategoryHeader layout
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false)
                CategoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateHeaderViewHolder -> holder.bind(items[position] as DateHeader)
            is CategoryViewHolder -> holder.bind(items[position] as CategoryHeader)
            is TaskViewHolder -> holder.bind(items[position] as TaskItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DateHeader -> 0 // Type for DateHeader
            is CategoryHeader -> 2 // Type for CategoryHeader
            is TaskItem -> 1 // Type for TaskItem
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    // ViewHolder for DateHeader
    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.tv_date_header)

        fun bind(dateHeader: DateHeader) {
            dateTextView.text = dateHeader.date
        }
    }

    // ViewHolder for CategoryHeader
    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val categoryTextView: TextView = view.findViewById(R.id.tv_category_header)

        fun bind(categoryHeader: CategoryHeader) {
            categoryTextView.text = categoryHeader.category
        }
    }

    // ViewHolder for TaskItem
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskName: TextView = itemView.findViewById(R.id.tv_task)
        private val taskTime: TextView = itemView.findViewById(R.id.tv_time)
        private val statusCheckBox: CheckBox = itemView.findViewById(R.id.statusCheckBox)

        fun bind(taskItem: TaskItem) {
            taskName.text = taskItem.name
            taskTime.text = taskItem.time
            statusCheckBox.isChecked = taskItem.isCompleted

            // Apply strike-through line if task is completed
            if (taskItem.isCompleted) {
                taskName.paintFlags = taskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskName.paintFlags = taskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Update Firestore when checkbox status changes
            statusCheckBox.setOnCheckedChangeListener { _, isChecked ->
                taskItem.isCompleted = isChecked
                updateTaskStatusInFirestore(taskItem)

                // Apply strike-through line when checked
                if (isChecked) {
                    taskName.paintFlags = taskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    taskName.paintFlags = taskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
        }
    }

    private fun updateTaskStatusInFirestore(taskItem: TaskItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("HomeFragment", "User not authenticated")
            return
        }

        val db = FirebaseFirestore.getInstance().collection("tasks")
        db.whereEqualTo("userId", userId)
            .whereEqualTo("taskName", taskItem.name) // Match the task by name
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    db.document(document.id)
                        .update("status", taskItem.isCompleted)
                        .addOnSuccessListener {
                            Log.d("HomeFragment", "Task status updated successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeFragment", "Failed to update task status.", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Failed to fetch task for update.", e)
            }
    }
}
