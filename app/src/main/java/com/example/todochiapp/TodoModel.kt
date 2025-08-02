package com.example.todochiapp

import com.google.firebase.Timestamp

data class TodoModel(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val timestamp: Long = 0L,
    val deadline: Timestamp? = null,
    val priority: String = ""
)


