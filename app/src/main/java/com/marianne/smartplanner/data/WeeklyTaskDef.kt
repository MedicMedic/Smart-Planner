package com.marianne.smartplanner.data

data class WeeklyTaskDef(
    val id: Int,
    val name: String,
    val group: String,
    val order: Int,
    val days: List<Int>  // ISO day-of-week: 1=Mon, 2=Tue, ..., 7=Sun
)
