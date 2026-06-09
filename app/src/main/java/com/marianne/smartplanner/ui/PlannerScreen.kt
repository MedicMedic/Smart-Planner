package com.marianne.smartplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marianne.smartplanner.data.*
import com.marianne.smartplanner.ui.components.*
import com.marianne.smartplanner.ui.theme.LocalAppColors
import com.marianne.smartplanner.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlannerScreen(vm: PlannerViewModel) {
    val c              = LocalAppColors.current
    val date           by vm.currentDate.collectAsStateWithLifecycle()
    val entry          by vm.entry.collectAsStateWithLifecycle()
    val routine        by vm.routine.collectAsStateWithLifecycle()
    val weeklyTasks    by vm.weeklyTasks.collectAsStateWithLifecycle()
    val timetableName  by vm.timetableName.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val todayWeeklyTasks = remember(weeklyTasks, date) {
        weeklyTasks.filter { date.dayOfWeek.value in it.days }
    }

    Scaffold(
        topBar = {
            DateNavigationBar(
                date           = date,
                onPrev         = vm::previousDay,
                onNext         = vm::nextDay,
                onPickDate     = { showDatePicker = true },
                onOpenSettings = vm::openSettings
            )
        },
        containerColor = c.pageBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timetableName,
                    style    = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = c.pink),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, "Rename timetable", tint = c.pink, modifier = Modifier.size(16.dp))
                }
            }
            AwakeRangeCard(entry, vm)
            RoutineChecklistCard(
                entry          = entry,
                routine        = routine,
                onToggle       = vm::toggleRoutineTask,
                onTimeChange   = vm::updateRoutineTaskTime,
                onOpenSettings = vm::openSettings
            )
            if (todayWeeklyTasks.isNotEmpty()) {
                WeeklyTasksCard(
                    entry        = entry,
                    tasks        = todayWeeklyTasks,
                    onToggle     = vm::toggleWeeklyTask,
                    onTimeChange = vm::updateWeeklyTaskTime,
                    onOpenSettings = vm::openSettings
                )
            }
            TodayGoalsCard(entry, vm)
            DailyDiaryCard(entry, vm::setDiary)
            MoodPickerCard(entry, vm::setMood)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate    = date,
            onDateSelected = { vm.goToDate(it); showDatePicker = false },
            onDismiss      = { showDatePicker = false }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            current   = timetableName,
            onConfirm = { vm.setTimetableName(it); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val c   = LocalAppColors.current
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Timetable name", color = c.pink, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.pink, unfocusedBorderColor = c.pinkLight)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text("OK", color = c.pink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.pink) } }
    )
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateNavigationBar(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val c       = LocalAppColors.current
    val label   = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))

    TopAppBar(
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.pink,
                    modifier = Modifier.clickable { onPickDate() })
                Text(dayName, fontSize = 12.sp, color = c.textSub)
            }
        },
        navigationIcon = {
            IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, "Previous day", tint = c.pink) }
        },
        actions = {
            IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next day", tint = c.pink) }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Customize routine", tint = c.pink) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = c.pageBg)
    )
}

// ── Awake range ───────────────────────────────────────────────────────────────

@Composable
private fun AwakeRangeCard(entry: DayEntry, vm: PlannerViewModel) {
    val c = LocalAppColors.current
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    SectionCard {
        SectionTitle("Awake Range")
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeChip("Wake",  entry.awakeStart, { showStartPicker = true }, Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = c.pink, modifier = Modifier.size(16.dp))
            TimeChip("Sleep", entry.awakeEnd,   { showEndPicker   = true }, Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Next", fontSize = 10.sp, color = c.textSub)
                Text("day",  fontSize = 10.sp, color = c.textSub)
                Switch(
                    checked         = entry.awakeEndNextDay,
                    onCheckedChange = { vm.setAwakeEndNextDay(it) },
                    colors  = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = c.pink),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
        entry.awakeMinutes?.let { minutes ->
            Spacer(Modifier.height(6.dp))
            Text("Awake for ${formatMinutes(minutes)}", fontSize = 13.sp, color = c.textSub, fontStyle = FontStyle.Italic)
        }
    }

    if (showStartPicker) TimePickerDialog("Wake-up time", entry.awakeStart, { vm.setAwakeStart(it); showStartPicker = false }, { showStartPicker = false })
    if (showEndPicker)   TimePickerDialog("Sleep time",   entry.awakeEnd,   { vm.setAwakeEnd(it);   showEndPicker   = false }, { showEndPicker   = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeChip(label: String, time: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp), color = c.pinkContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = c.textSub)
            Text(time.ifBlank { "--:--" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.pink)
        }
    }
}

// ── Daily routine ─────────────────────────────────────────────────────────────

@Composable
private fun RoutineChecklistCard(
    entry: DayEntry,
    routine: List<RoutineTaskDef>,
    onToggle: (Int) -> Unit,
    onTimeChange: (Int, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val c          = LocalAppColors.current
    val groupOrder = remember(routine) { routine.sortedBy { it.order }.map { it.group }.distinct() }
    val grouped    = remember(routine) { routine.groupBy { it.group } }
    val done       = routine.count { entry.isRoutineTaskChecked(it.id) }
    val total      = routine.size
    var timePickerTaskId by remember { mutableStateOf<Int?>(null) }

    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Daily Routine")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (total > 0) Text("$done/$total", fontSize = 13.sp, color = c.pink, fontWeight = FontWeight.Bold)
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Tune, "Customize", tint = c.pink, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (total > 0) {
            LinearProgressIndicator(
                progress   = { done / total.toFloat() },
                modifier   = Modifier.fillMaxWidth().height(6.dp),
                color      = c.pink,
                trackColor = c.pinkLight
            )
            Spacer(Modifier.height(8.dp))
        }

        if (routine.isEmpty()) {
            Text("No routine tasks yet. Tap ⚙ to customize.", fontSize = 13.sp, color = c.textSub, fontStyle = FontStyle.Italic)
        }

        groupOrder.forEach { group ->
            val tasks = (grouped[group] ?: emptyList()).sortedBy { it.order }
            Text(group, fontSize = 12.sp, color = c.pink, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            tasks.forEach { task ->
                val checked = entry.isRoutineTaskChecked(task.id)
                CheckRow(
                    checked     = checked,
                    onToggle    = { onToggle(task.id) },
                    label       = task.name,
                    completedAt = if (checked) entry.routineCheckTimes[task.id] else null,
                    onTimeClick = { timePickerTaskId = task.id }
                )
            }
        }
    }

    timePickerTaskId?.let { taskId ->
        TimePickerDialog(
            title     = routine.find { it.id == taskId }?.name ?: "",
            initial   = entry.routineCheckTimes[taskId] ?: "",
            onConfirm = { onTimeChange(taskId, it); timePickerTaskId = null },
            onDismiss = { timePickerTaskId = null }
        )
    }
}

// ── Weekly routine ────────────────────────────────────────────────────────────

@Composable
private fun WeeklyTasksCard(
    entry: DayEntry,
    tasks: List<WeeklyTaskDef>,
    onToggle: (Int) -> Unit,
    onTimeChange: (Int, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val c          = LocalAppColors.current
    val groupOrder = remember(tasks) { tasks.sortedBy { it.order }.map { it.group }.distinct() }
    val grouped    = remember(tasks) { tasks.groupBy { it.group } }
    val done       = tasks.count { entry.isWeeklyTaskChecked(it.id) }
    val total      = tasks.size
    var timePickerTaskId by remember { mutableStateOf<Int?>(null) }

    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Weekly Tasks")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (total > 0) Text("$done/$total", fontSize = 13.sp, color = c.pink, fontWeight = FontWeight.Bold)
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Tune, "Customize", tint = c.pink, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (total > 0) {
            LinearProgressIndicator(
                progress   = { done / total.toFloat() },
                modifier   = Modifier.fillMaxWidth().height(6.dp),
                color      = c.pink,
                trackColor = c.pinkLight
            )
            Spacer(Modifier.height(8.dp))
        }

        groupOrder.forEach { group ->
            val groupTasks = (grouped[group] ?: emptyList()).sortedBy { it.order }
            Text(group, fontSize = 12.sp, color = c.pink, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            groupTasks.forEach { task ->
                val checked = entry.isWeeklyTaskChecked(task.id)
                CheckRow(
                    checked     = checked,
                    onToggle    = { onToggle(task.id) },
                    label       = task.name,
                    completedAt = if (checked) entry.weeklyCheckTimes[task.id] else null,
                    onTimeClick = { timePickerTaskId = task.id }
                )
            }
        }
    }

    timePickerTaskId?.let { taskId ->
        TimePickerDialog(
            title     = tasks.find { it.id == taskId }?.name ?: "",
            initial   = entry.weeklyCheckTimes[taskId] ?: "",
            onConfirm = { onTimeChange(taskId, it); timePickerTaskId = null },
            onDismiss = { timePickerTaskId = null }
        )
    }
}

// ── Today's goals ─────────────────────────────────────────────────────────────

@Composable
private fun TodayGoalsCard(entry: DayEntry, vm: PlannerViewModel) {
    val c    = LocalAppColors.current
    val done = entry.todayGoals.count { it.checked }
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Today's Goals")
            if (entry.todayGoals.isNotEmpty()) Text("$done/${entry.todayGoals.size}", fontSize = 13.sp, color = c.pink, fontWeight = FontWeight.Bold)
        }

        if (entry.todayGoals.isEmpty()) {
            Text("No goals yet — tap + to add one", fontSize = 13.sp, color = c.textSub, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(4.dp))
        }

        entry.todayGoals.forEach { goal ->
            GoalRow(
                goal         = goal,
                onToggle     = { vm.toggleGoal(goal.id) },
                onNameChange = { vm.updateGoalName(goal.id, it) },
                onDelete     = { vm.deleteGoal(goal.id) },
                onTimeChange = { vm.updateGoalTime(goal.id, it) }
            )
        }

        TextButton(onClick = vm::addGoal, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
            Icon(Icons.Default.Add, null, tint = c.pink, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add goal", fontSize = 13.sp, color = c.pink)
        }
    }
}

@Composable
private fun GoalRow(
    goal: Goal,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onDelete: () -> Unit,
    onTimeChange: (String) -> Unit
) {
    val c = LocalAppColors.current
    var showTimePicker by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector        = if (goal.checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint               = c.pink,
            modifier           = Modifier.size(22.dp).clickable { onToggle() }
        )
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value         = goal.name,
            onValueChange = onNameChange,
            singleLine    = true,
            textStyle     = TextStyle(
                fontSize       = 14.sp,
                color          = if (goal.checked) c.textSub else c.textMain,
                textDecoration = if (goal.checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush   = SolidColor(c.pink),
            decorationBox = { inner ->
                if (goal.name.isEmpty()) Text("Goal description…", fontSize = 14.sp, color = c.textSub)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
        if (goal.checked && goal.completedAt != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                goal.completedAt,
                fontSize   = 11.sp,
                color      = c.pink,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .background(c.pinkContainer, RoundedCornerShape(4.dp))
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, "Remove", tint = c.textSub, modifier = Modifier.size(16.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title     = goal.name.ifBlank { "Goal" },
            initial   = goal.completedAt ?: "",
            onConfirm = { onTimeChange(it); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ── Daily diary ───────────────────────────────────────────────────────────────

@Composable
private fun DailyDiaryCard(entry: DayEntry, onDiaryChange: (String) -> Unit) {
    val c = LocalAppColors.current
    SectionCard {
        SectionTitle("Daily Diary")
        OutlinedTextField(
            value         = entry.diary,
            onValueChange = onDiaryChange,
            modifier      = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle     = TextStyle(fontSize = 14.sp, color = c.textMain),
            placeholder   = { Text("Write about your day…", fontSize = 14.sp, color = c.textSub) },
            colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.pink, unfocusedBorderColor = c.pinkLight),
            shape         = RoundedCornerShape(8.dp)
        )
    }
}

// ── Mood picker ───────────────────────────────────────────────────────────────

@Composable
private fun MoodPickerCard(entry: DayEntry, onMoodSelected: (Int) -> Unit) {
    val c = LocalAppColors.current
    SectionCard {
        SectionTitle("Mood")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Mood.entries.forEach { mood ->
                val selected = entry.moodOrdinal == mood.ordinal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.clickable { onMoodSelected(mood.ordinal) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(48.dp)
                            .background(if (selected) c.pinkContainer else Color.Transparent, CircleShape)
                            .border(if (selected) 2.dp else 0.dp, if (selected) c.pink else Color.Transparent, CircleShape)
                    ) {
                        Text(mood.emoji, fontSize = 26.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(mood.label, fontSize = 10.sp,
                        color      = if (selected) c.pink else c.textSub,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c     = LocalAppColors.current
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onDateSelected(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("OK", color = c.pink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.pink) } }
    ) {
        DatePicker(state = state, colors = DatePickerDefaults.colors(selectedDayContainerColor = c.pink, todayDateBorderColor = c.pink))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val c     = LocalAppColors.current
    val parts = initial.split(":").mapNotNull { it.toIntOrNull() }
    val now   = remember { LocalTime.now() }
    val state = rememberTimePickerState(
        initialHour   = parts.getOrElse(0) { now.hour },
        initialMinute = parts.getOrElse(1) { now.minute },
        is24Hour      = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title, color = c.pink, fontWeight = FontWeight.Bold) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state, colors = TimePickerDefaults.colors(
                    clockDialColor                    = c.pinkContainer,
                    selectorColor                     = c.pink,
                    timeSelectorSelectedContainerColor = c.pink,
                    timeSelectorSelectedContentColor  = Color.White
                ))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
            }) { Text("OK", color = c.pink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.pink) } }
    )
}
