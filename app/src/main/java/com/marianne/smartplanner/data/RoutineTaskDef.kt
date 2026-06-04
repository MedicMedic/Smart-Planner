package com.marianne.smartplanner.data

data class RoutineTaskDef(
    val id: Int,
    val name: String,
    val group: String,
    val order: Int
)

val DEFAULT_ROUTINE_TASKS: List<RoutineTaskDef> = listOf(
    RoutineTaskDef(1,  "Wake up",                "Morning",   0),
    RoutineTaskDef(2,  "Breakfast",              "Morning",   1),
    RoutineTaskDef(3,  "Chores",                 "Morning",   2),
    RoutineTaskDef(4,  "Bath & Brush",           "Morning",   3),
    RoutineTaskDef(5,  "Lunch",                  "Afternoon", 4),
    RoutineTaskDef(6,  "Brush teeth",            "Afternoon", 5),
    RoutineTaskDef(7,  "Chores",                 "Afternoon", 6),
    RoutineTaskDef(8,  "Dinner",                 "Night",     7),
    RoutineTaskDef(9,  "Chores",                 "Night",     8),
    RoutineTaskDef(10, "Skincare & Brush teeth", "Night",     9),
    RoutineTaskDef(11, "Pray",                   "Night",     10),
    RoutineTaskDef(12, "Sleep",                  "Night",     11),
)
