package com.example.taskmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val taskNameTextView: TextView = view.findViewById(R.id.tv_task)
        private val taskTimeTextView: TextView = view.findViewById(R.id.tv_time)

        fun bind(taskItem: TaskItem) {
            taskNameTextView.text = taskItem.taskName
            taskTimeTextView.text = taskItem.time
        }
    }
}
