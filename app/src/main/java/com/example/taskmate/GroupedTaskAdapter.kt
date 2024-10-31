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
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateHeaderViewHolder -> holder.bind(items[position] as DateHeader)
            is TaskViewHolder -> holder.bind(items[position] as TaskItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position] is DateHeader) {
            true -> 0 // Type for DateHeader
            false -> 1 // Type for TaskItem
        }
    }

    override fun getItemCount(): Int = items.size

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.tv_date_header)

        fun bind(dateHeader: DateHeader) {
            dateTextView.text = dateHeader.date
        }
    }

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val taskNameTextView: TextView = view.findViewById(R.id.tv_task)
        private val taskTimeTextView: TextView = view.findViewById(R.id.tv_time)

        fun bind(taskItem: TaskItem) {
            taskNameTextView.text = taskItem.taskName
            taskTimeTextView.text = taskItem.time
        }
    }
}
