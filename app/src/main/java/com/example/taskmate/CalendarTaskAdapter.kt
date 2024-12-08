package com.example.taskmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CalendarTaskAdapter(private var groupedTaskList: List<Pair<String, List<CalenderTask>>>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_TASK = 1
    }

    // ViewHolder for Category Header
    inner class CategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.tv_category_header)
    }

    // ViewHolder for Task Item
    inner class TaskItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.tv_task_title)
        val taskTime: TextView = itemView.findViewById(R.id.tv_time)
    }

    // Create ViewHolder depending on the type of view (category or task item)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            TYPE_TASK -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_task, parent, false)
                TaskItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // Bind data to ViewHolder depending on the view type
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentIndex = 0
        for (group in groupedTaskList) {
            val categorySize = group.second.size + 1 // 1 for the category header
            if (position == currentIndex) {
                // This is the category header position
                (holder as CategoryHeaderViewHolder).categoryTitle.text = group.first
                return
            } else if (position < currentIndex + group.second.size + 1) {
                // This is a task item under the current category
                val task = group.second[position - currentIndex - 1]
                (holder as TaskItemViewHolder).taskTitle.text = task.taskName
                holder.taskTime.text = formatTime(task.deadlineAndTime)
                return
            }
            currentIndex += categorySize
        }
    }

    // Get the view type for each item (category or task)
    override fun getItemViewType(position: Int): Int {
        var currentIndex = 0
        for (group in groupedTaskList) {
            val categorySize = group.second.size + 1 // 1 for the category header
            if (position == currentIndex) {
                return TYPE_HEADER
            } else if (position < currentIndex + group.second.size + 1) {
                return TYPE_TASK
            }
            currentIndex += categorySize
        }
        throw IllegalArgumentException("Invalid position")
    }

    override fun getItemCount(): Int {
        var count = 0
        for (group in groupedTaskList) {
            count += group.second.size + 1 // One for the category header
        }
        return count
    }

    // Update the task list with grouped data
    fun updateTasks(newGroupedTasks: List<Pair<String, List<CalenderTask>>>) {
        groupedTaskList = newGroupedTasks
        notifyDataSetChanged()
    }

    // Format the time as HH.mm
    private fun formatTime(deadlineAndTime: String): String {
        try {
            val format = SimpleDateFormat("HH:mm, d MMMM yyyy", Locale("id", "ID"))
            val date = format.parse(deadlineAndTime)
            val timeFormat = SimpleDateFormat("HH.mm", Locale("id", "ID"))
            return timeFormat.format(date ?: Date())
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
