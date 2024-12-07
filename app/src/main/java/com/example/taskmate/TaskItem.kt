package com.example.taskmate

data class TaskItem(
    val id: String,
    val name: String,
    val time: String,
    var isCompleted: Boolean = false
) : ListItem


