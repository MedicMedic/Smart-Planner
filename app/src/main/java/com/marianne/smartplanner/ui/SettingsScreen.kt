package com.marianne.smartplanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marianne.smartplanner.data.RoutineTaskDef
import com.marianne.smartplanner.data.WeeklyTaskDef
import com.marianne.smartplanner.ui.theme.LocalAppColors
import com.marianne.smartplanner.viewmodel.PlannerViewModel

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: PlannerViewModel) {
    BackHandler { vm.closeSettings() }
    val c                  = LocalAppColors.current
    val context            = LocalContext.current
    val dailyTasks         by vm.routine.collectAsStateWithLifecycle()
    val weeklyTasks        by vm.weeklyTasks.collectAsStateWithLifecycle()
    val notifEnabled       by vm.notificationsEnabled.collectAsStateWithLifecycle()
    val notifTimes         by vm.notificationTimes.collectAsStateWithLifecycle()
    var selectedTab        by remember { mutableIntStateOf(0) }

    // Daily dialog states
    var showAddDailyGroup       by remember { mutableStateOf(false) }
    var addDailyTaskToGroup     by remember { mutableStateOf<String?>(null) }
    var renameDailyGroup        by remember { mutableStateOf<String?>(null) }
    var confirmDeleteDailyGroup by remember { mutableStateOf<String?>(null) }

    // Weekly dialog states
    var showAddWeeklyGroup       by remember { mutableStateOf(false) }
    var addWeeklyTaskToGroup     by remember { mutableStateOf<String?>(null) }
    var renameWeeklyGroup        by remember { mutableStateOf<String?>(null) }
    var editWeeklyGroupDays      by remember { mutableStateOf<String?>(null) }
    var confirmDeleteWeeklyGroup by remember { mutableStateOf<String?>(null) }

    // Reminder dialog state
    var editReminderIndex by remember { mutableStateOf<Int?>(null) }
    var showAddReminder   by remember { mutableStateOf(false) }

    val dailyGroupOrder  = remember(dailyTasks)  { dailyTasks.sortedBy { it.order }.map { it.group }.distinct() }
    val dailyGrouped     = remember(dailyTasks)  { dailyTasks.groupBy { it.group } }
    val weeklyGroupOrder = remember(weeklyTasks) { weeklyTasks.sortedBy { it.order }.map { it.group }.distinct() }
    val weeklyGrouped    = remember(weeklyTasks) { weeklyTasks.groupBy { it.group } }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.setNotificationsEnabled(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Routine", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.pink) },
                navigationIcon = {
                    IconButton(onClick = vm::closeSettings) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.pink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.pageBg)
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { showAddDailyGroup = true },
                    containerColor = c.pink, contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add group") }
                )
                1 -> ExtendedFloatingActionButton(
                    onClick = { showAddWeeklyGroup = true },
                    containerColor = c.pink, contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add weekly group") }
                )
                else -> {}
            }
        },
        containerColor = c.pageBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = c.pageBg, contentColor = c.pink) {
                listOf("Daily", "Weekly", "Reminders", "Backup").forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                // ── Daily ─────────────────────────────────────────────────────
                0 -> if (dailyTasks.isEmpty()) {
                    EmptyHint("No routine tasks yet.\nTap \"Add group\" to get started.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                    ) {
                        dailyGroupOrder.forEach { group ->
                            val tasks = (dailyGrouped[group] ?: emptyList()).sortedBy { it.order }
                            item(key = "daily_$group") {
                                GroupCard(
                                    groupName     = group,
                                    tasks         = tasks,
                                    onAddTask     = { addDailyTaskToGroup = group },
                                    onRenameGroup = { renameDailyGroup = group },
                                    onDeleteGroup = { confirmDeleteDailyGroup = group },
                                    onRenameTask  = { id, n -> vm.updateRoutineTaskName(id, n) },
                                    onDeleteTask  = { vm.deleteRoutineTask(it) },
                                    onMoveUp      = { vm.moveRoutineTaskUp(it) },
                                    onMoveDown    = { vm.moveRoutineTaskDown(it) }
                                )
                            }
                        }
                    }
                }

                // ── Weekly ────────────────────────────────────────────────────
                1 -> if (weeklyTasks.isEmpty()) {
                    EmptyHint("No weekly tasks yet.\nTap \"Add weekly group\" to get started.\nExample: \"Sunday\" with Church.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                    ) {
                        weeklyGroupOrder.forEach { group ->
                            val tasks     = (weeklyGrouped[group] ?: emptyList()).sortedBy { it.order }
                            val groupDays = tasks.firstOrNull()?.days ?: emptyList()
                            item(key = "weekly_$group") {
                                WeeklyGroupCard(
                                    groupName     = group,
                                    days          = groupDays,
                                    tasks         = tasks,
                                    onAddTask     = { addWeeklyTaskToGroup = group },
                                    onRenameGroup = { renameWeeklyGroup = group },
                                    onEditDays    = { editWeeklyGroupDays = group },
                                    onDeleteGroup = { confirmDeleteWeeklyGroup = group },
                                    onRenameTask  = { id, n -> vm.updateWeeklyTaskName(id, n) },
                                    onDeleteTask  = { vm.deleteWeeklyTask(it) },
                                    onMoveUp      = { vm.moveWeeklyTaskUp(it) },
                                    onMoveDown    = { vm.moveWeeklyTaskDown(it) }
                                )
                            }
                        }
                    }
                }

                // ── Backup ───────────────────────────────────────────────────
                3 -> BackupTab(vm)

                // ── Reminders ─────────────────────────────────────────────────
                else -> RemindersTab(
                    enabled       = notifEnabled,
                    times         = notifTimes,
                    onToggle      = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setNotificationsEnabled(true)
                            }
                        } else {
                            vm.setNotificationsEnabled(false)
                        }
                    },
                    onAddTime     = { showAddReminder = true },
                    onEditTime    = { editReminderIndex = it },
                    onRemoveTime  = { vm.removeNotificationTime(it) }
                )
            }
        }
    }

    // ── Daily dialogs ─────────────────────────────────────────────────────────

    if (showAddDailyGroup) {
        InputDialog("New group", "e.g. Evening",
            onConfirm = { vm.addGroup(it); showAddDailyGroup = false },
            onDismiss = { showAddDailyGroup = false })
    }
    addDailyTaskToGroup?.let { group ->
        InputDialog("Add task to \"$group\"", "Task name",
            onConfirm = { vm.addRoutineTask(it, group); addDailyTaskToGroup = null },
            onDismiss = { addDailyTaskToGroup = null })
    }
    renameDailyGroup?.let { old ->
        InputDialog("Rename group", old, initialValue = old,
            onConfirm = { vm.renameGroup(old, it); renameDailyGroup = null },
            onDismiss = { renameDailyGroup = null })
    }
    confirmDeleteDailyGroup?.let { group ->
        ConfirmDeleteDialog(group, dailyGrouped[group]?.size ?: 0,
            onConfirm = { vm.deleteGroup(group); confirmDeleteDailyGroup = null },
            onDismiss = { confirmDeleteDailyGroup = null })
    }

    // ── Weekly dialogs ────────────────────────────────────────────────────────

    if (showAddWeeklyGroup) {
        AddWeeklyGroupDialog(
            onConfirm = { name, days -> vm.addWeeklyGroup(name, days); showAddWeeklyGroup = false },
            onDismiss = { showAddWeeklyGroup = false })
    }
    addWeeklyTaskToGroup?.let { group ->
        val days = weeklyGrouped[group]?.firstOrNull()?.days ?: emptyList()
        InputDialog("Add task to \"$group\"", "Task name",
            onConfirm = { vm.addWeeklyTask(it, group, days); addWeeklyTaskToGroup = null },
            onDismiss = { addWeeklyTaskToGroup = null })
    }
    renameWeeklyGroup?.let { old ->
        InputDialog("Rename group", old, initialValue = old,
            onConfirm = { vm.renameWeeklyGroup(old, it); renameWeeklyGroup = null },
            onDismiss = { renameWeeklyGroup = null })
    }
    editWeeklyGroupDays?.let { group ->
        EditDaysDialog(group, weeklyGrouped[group]?.firstOrNull()?.days ?: emptyList(),
            onConfirm = { vm.setWeeklyGroupDays(group, it); editWeeklyGroupDays = null },
            onDismiss = { editWeeklyGroupDays = null })
    }
    confirmDeleteWeeklyGroup?.let { group ->
        ConfirmDeleteDialog(group, weeklyGrouped[group]?.size ?: 0,
            onConfirm = { vm.deleteWeeklyGroup(group); confirmDeleteWeeklyGroup = null },
            onDismiss = { confirmDeleteWeeklyGroup = null })
    }

    // ── Reminder dialogs ──────────────────────────────────────────────────────

    if (showAddReminder) {
        TimePickerDialog("Add reminder", "",
            onConfirm = { vm.addNotificationTime(it); showAddReminder = false },
            onDismiss = { showAddReminder = false })
    }
    editReminderIndex?.let { idx ->
        TimePickerDialog("Edit reminder", notifTimes.getOrElse(idx) { "" },
            onConfirm = { vm.updateNotificationTime(idx, it); editReminderIndex = null },
            onDismiss = { editReminderIndex = null })
    }
}

// ── Backup tab ────────────────────────────────────────────────────────────────

@Composable
private fun BackupTab(vm: PlannerViewModel) {
    val c       = LocalAppColors.current
    val context = LocalContext.current
    var status  by remember { mutableStateOf<String?>(null) }
    var pendingUri     by remember { mutableStateOf<Uri?>(null) }
    var showConfirm    by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = vm.exportBackup()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            status = "Exported successfully."
        } catch (e: Exception) {
            status = "Export failed: ${e.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingUri  = uri
        showConfirm = true
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = c.cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Export data", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                        Text("Save a backup .json file", fontSize = 12.sp, color = c.textSub)
                    }
                    Button(
                        onClick = { exportLauncher.launch("smartplanner_backup.json") },
                        colors  = ButtonDefaults.buttonColors(containerColor = c.pink)
                    ) { Text("Export") }
                }
            }
        }

        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = c.cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Import data", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                        Text("Replaces all current data", fontSize = 12.sp, color = c.textSub)
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                        colors  = ButtonDefaults.buttonColors(containerColor = c.pink)
                    ) { Text("Import") }
                }
            }
        }

        status?.let { msg ->
            item {
                Text(
                    msg,
                    fontSize = 13.sp,
                    color    = if (msg.endsWith("successfully.")) c.pink else Color.Red,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false; pendingUri = null },
            title   = { Text("Import backup?", color = c.pink, fontWeight = FontWeight.Bold) },
            text    = { Text("This will replace ALL your current data with the backup. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingUri
                    if (uri != null) {
                        try {
                            val json = context.contentResolver.openInputStream(uri)
                                ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                            vm.importBackup(json)
                            status = "Imported successfully."
                        } catch (e: Exception) {
                            status = "Import failed: ${e.message}"
                        }
                    }
                    showConfirm = false
                    pendingUri  = null
                }) { Text("Import", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false; pendingUri = null }) {
                    Text("Cancel", color = c.pink)
                }
            }
        )
    }
}

// ── Reminders tab ─────────────────────────────────────────────────────────────

@Composable
private fun RemindersTab(
    enabled: Boolean,
    times: List<String>,
    onToggle: (Boolean) -> Unit,
    onAddTime: () -> Unit,
    onEditTime: (Int) -> Unit,
    onRemoveTime: (Int) -> Unit
) {
    val c = LocalAppColors.current
    LazyColumn(
        modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = c.cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable reminders", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                        Text("\"Log your day!\" notification", fontSize = 12.sp, color = c.textSub)
                    }
                    Switch(
                        checked         = enabled,
                        onCheckedChange = onToggle,
                        colors          = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = c.pink)
                    )
                }
            }
        }

        if (enabled) {
            if (times.isEmpty()) {
                item {
                    Text(
                        "No reminders set. Tap + to add one.",
                        fontSize = 13.sp, color = c.textSub, fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            times.forEachIndexed { index, time ->
                item(key = "reminder_$index") {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = c.cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = c.pink, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(time, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.pink, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onEditTime(index) }) {
                                Icon(Icons.Default.Edit, "Edit time", tint = c.pink, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onRemoveTime(index) }) {
                                Icon(Icons.Default.Close, "Remove", tint = c.textSub, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = onAddTime,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = c.pink, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add reminder", fontSize = 13.sp, color = c.pink)
                }
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun EmptyHint(text: String) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = c.textSub, fontSize = 14.sp, fontStyle = FontStyle.Italic)
    }
}

// ── Daily GroupCard ───────────────────────────────────────────────────────────

@Composable
private fun GroupCard(
    groupName: String,
    tasks: List<RoutineTaskDef>,
    onAddTask: () -> Unit,
    onRenameGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onRenameTask: (Int, String) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    val c = LocalAppColors.current
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = c.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(groupName, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = c.pink)
                Row {
                    IconButton(onClick = onRenameGroup, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Rename", tint = c.pink, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteGroup, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = c.textSub, modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = c.pinkLight, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
            tasks.forEachIndexed { idx, task ->
                TaskRow(task, idx == 0, idx == tasks.lastIndex,
                    onRename   = { onRenameTask(task.id, it) },
                    onDelete   = { onDeleteTask(task.id) },
                    onMoveUp   = { onMoveUp(task.id) },
                    onMoveDown = { onMoveDown(task.id) })
            }
            if (tasks.isEmpty()) {
                Text("No tasks in this group.", fontSize = 13.sp, color = c.textSub, fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 4.dp))
            }
            TextButton(onClick = onAddTask, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Add, null, tint = c.pink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add task", fontSize = 13.sp, color = c.pink)
            }
        }
    }
}

// ── Weekly GroupCard ──────────────────────────────────────────────────────────

@Composable
private fun WeeklyGroupCard(
    groupName: String,
    days: List<Int>,
    tasks: List<WeeklyTaskDef>,
    onAddTask: () -> Unit,
    onRenameGroup: () -> Unit,
    onEditDays: () -> Unit,
    onDeleteGroup: () -> Unit,
    onRenameTask: (Int, String) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    val c        = LocalAppColors.current
    val daysText = if (days.isEmpty()) "No days" else days.sorted().joinToString(" · ") { DAY_LABELS[it - 1] }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = c.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(groupName, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = c.pink)
                    Text(daysText, fontSize = 11.sp, color = c.textSub)
                }
                Row {
                    IconButton(onClick = onEditDays, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.CalendarMonth, "Edit days", tint = c.pink, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRenameGroup, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Rename", tint = c.pink, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteGroup, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = c.textSub, modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = c.pinkLight, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
            tasks.forEachIndexed { idx, task ->
                WeeklyTaskRow(task, idx == 0, idx == tasks.lastIndex,
                    onRename   = { onRenameTask(task.id, it) },
                    onDelete   = { onDeleteTask(task.id) },
                    onMoveUp   = { onMoveUp(task.id) },
                    onMoveDown = { onMoveDown(task.id) })
            }
            if (tasks.isEmpty()) {
                Text("No tasks in this group.", fontSize = 13.sp, color = c.textSub, fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 4.dp))
            }
            TextButton(onClick = onAddTask, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Add, null, tint = c.pink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add task", fontSize = 13.sp, color = c.pink)
            }
        }
    }
}

// ── Task rows ─────────────────────────────────────────────────────────────────

@Composable
private fun TaskRow(
    task: RoutineTaskDef,
    isFirst: Boolean,
    isLast: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val c    = LocalAppColors.current
    var name by remember(task.id, task.name) { mutableStateOf(task.name) }
    LaunchedEffect(name) { if (name != task.name && name.isNotBlank()) onRename(name) }

    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Up", tint = if (isFirst) c.textSub else c.pink, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Down", tint = if (isLast) c.textSub else c.pink, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = name, onValueChange = { name = it }, singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = c.textMain),
            cursorBrush = SolidColor(c.pink), modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Column {
                    Box(Modifier.padding(bottom = 2.dp)) {
                        if (name.isEmpty()) Text("Task name…", fontSize = 14.sp, color = c.textSub)
                        inner()
                    }
                    HorizontalDivider(color = c.pinkLight, thickness = 1.dp)
                }
            }
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, "Delete", tint = c.textSub, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun WeeklyTaskRow(
    task: WeeklyTaskDef,
    isFirst: Boolean,
    isLast: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val c    = LocalAppColors.current
    var name by remember(task.id, task.name) { mutableStateOf(task.name) }
    LaunchedEffect(name) { if (name != task.name && name.isNotBlank()) onRename(name) }

    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Up", tint = if (isFirst) c.textSub else c.pink, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Down", tint = if (isLast) c.textSub else c.pink, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = name, onValueChange = { name = it }, singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = c.textMain),
            cursorBrush = SolidColor(c.pink), modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Column {
                    Box(Modifier.padding(bottom = 2.dp)) {
                        if (name.isEmpty()) Text("Task name…", fontSize = 14.sp, color = c.textSub)
                        inner()
                    }
                    HorizontalDivider(color = c.pinkLight, thickness = 1.dp)
                }
            }
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, "Delete", tint = c.textSub, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun InputDialog(
    title: String,
    hint: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c    = LocalAppColors.current
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = c.pink, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text(hint, color = c.textSub) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.pink, unfocusedBorderColor = c.pinkLight)
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

@Composable
private fun AddWeeklyGroupDialog(onConfirm: (String, List<Int>) -> Unit, onDismiss: () -> Unit) {
    val c            = LocalAppColors.current
    var name         by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New weekly group", color = c.pink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("e.g. Sunday", color = c.textSub) },
                    label = { Text("Group name", color = c.textSub) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.pink, unfocusedBorderColor = c.pinkLight)
                )
                Text("Appears on:", fontSize = 13.sp, color = c.textSub)
                DayChipRow(selectedDays) { day ->
                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank() && selectedDays.isNotEmpty()) onConfirm(name.trim(), selectedDays.sorted()) },
                enabled  = name.isNotBlank() && selectedDays.isNotEmpty()
            ) { Text("OK", color = c.pink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.pink) } }
    )
}

@Composable
private fun EditDaysDialog(groupName: String, currentDays: List<Int>, onConfirm: (List<Int>) -> Unit, onDismiss: () -> Unit) {
    val c            = LocalAppColors.current
    var selectedDays by remember { mutableStateOf(currentDays.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Days for \"$groupName\"", color = c.pink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appears on:", fontSize = 13.sp, color = c.textSub)
                DayChipRow(selectedDays) { day ->
                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selectedDays.isNotEmpty()) onConfirm(selectedDays.sorted()) },
                enabled = selectedDays.isNotEmpty()
            ) { Text("OK", color = c.pink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.pink) } }
    )
}

@Composable
private fun ConfirmDeleteDialog(group: String, taskCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Delete \"$group\"?", color = c.pink, fontWeight = FontWeight.Bold) },
        text             = { Text("This removes all $taskCount task(s) in this group permanently.") },
        confirmButton    = { TextButton(onClick = onConfirm) { Text("Delete", color = Color.Red) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.pink) } }
    )
}

@Composable
private fun DayChipRow(selectedDays: Set<Int>, onToggle: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        (1..7).forEach { day ->
            val selected = day in selectedDays
            FilterChip(
                selected = selected,
                onClick  = { onToggle(day) },
                label    = { Text(DAY_LABELS[day - 1], fontSize = 11.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = c.pink,
                    selectedLabelColor     = Color.White,
                    containerColor         = c.pinkContainer,
                    labelColor             = c.pink
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
