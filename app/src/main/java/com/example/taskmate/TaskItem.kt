package com.example.taskmate

data class TaskItem(
    val name: String,
    val time: String,
    var isCompleted: Boolean = false
) : ListItem


