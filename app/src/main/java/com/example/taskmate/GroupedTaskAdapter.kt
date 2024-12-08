package com.example.taskmate

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class GroupedTaskAdapter(private val items: List<ListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> DateHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false))
            1 -> TaskViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))
            2 -> CategoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false))
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is TaskItem -> (holder as TaskViewHolder).bind(item)
            is CategoryHeader -> (holder as CategoryViewHolder).bind(item)
            else -> Log.e("GroupedTaskAdapter", "Skipped invalid item at position $position: $item")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is DateHeader -> 0
            is TaskItem -> 1
            is CategoryHeader -> 2
            else -> {
                Log.e("GroupedTaskAdapter", "Invalid item type at position $position: $item")
                -1
            }
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
    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val taskName: TextView = view.findViewById(R.id.tv_task)
        private val taskCheckBox: CheckBox = view.findViewById(R.id.statusCheckBox)

        fun bind(taskItem: TaskItem) {
            taskName.text = taskItem.name
            taskCheckBox.isChecked = taskItem.isCompleted
            taskName.paintFlags = if (taskItem.isCompleted) {
                taskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            taskCheckBox.setOnCheckedChangeListener(null)
            taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                taskItem.isCompleted = isChecked
                updateTaskStatusInFirestore(taskItem)
            }
        }

        private fun updateTaskStatusInFirestore(taskItem: TaskItem) {
            FirebaseFirestore.getInstance().collection("tasks").document(taskItem.id)
                .update("status", taskItem.isCompleted)
                .addOnSuccessListener { Log.d("GroupedTaskAdapter", "Task updated successfully") }
                .addOnFailureListener { Log.e("GroupedTaskAdapter", "Failed to update task", it) }
        }
    }
}
