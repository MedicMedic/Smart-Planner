package com.marianne.smartplanner.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marianne.smartplanner.NotificationScheduler
import com.marianne.smartplanner.data.RoutineTaskDef
import com.marianne.smartplanner.data.WeeklyTaskDef
import com.marianne.smartplanner.ui.theme.LocalAppColors
import com.marianne.smartplanner.ui.theme.ThemePresets
import com.marianne.smartplanner.viewmodel.PlannerViewModel

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

// ── Theme color ───────────────────────────────────────────────────────────────

private const val CUSTOM_SWATCH = "custom"
private val RAINBOW = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 0.85f, 0.8f) }

@Composable
private fun ThemeColorCard(themeId: String, onSelect: (String) -> Unit) {
    val c        = LocalAppColors.current
    val isCustom = ThemePresets.isCustom(themeId)

    // Custom starts from whatever color is active now; the sliders and hex field edit it.
    val picked = if (isCustom) ThemePresets.customColor(themeId) else ThemePresets.find(themeId).light
    val hsv    = FloatArray(3).also { android.graphics.Color.colorToHSV(picked.toArgb(), it) }
    val hue    = hsv[0]
    val sat    = if (hsv[1] < 0.15f) 0.85f else hsv[1]   // dragging hue on a gray should show color
    val value  = hsv[2].coerceIn(0.35f, 0.8f)
    var hexText by remember(themeId) { mutableStateOf("%06X".format(picked.toArgb() and 0xFFFFFF)) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = c.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Theme color", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                Text("Pick a preset, or use the rainbow for any color.", fontSize = 12.sp, color = c.textSub)
            }

            (ThemePresets.all.map { it.id } + CUSTOM_SWATCH).chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { id ->
                        val custom = id == CUSTOM_SWATCH
                        ColorSwatch(
                            label    = if (custom) "Custom color" else ThemePresets.find(id).label,
                            color    = if (custom) null else ThemePresets.find(id).light,
                            brush    = if (custom) Brush.sweepGradient(RAINBOW) else null,
                            selected = if (custom) isCustom else themeId == id,
                            onClick  = { onSelect(if (custom) ThemePresets.customId(picked) else id) }
                        )
                    }
                }
            }

            if (isCustom) {
                Text("Color", fontSize = 12.sp, color = c.textSub)
                GradientSlider(
                    fraction = hue / 360f,
                    brush    = Brush.horizontalGradient(RAINBOW),
                    onChange = { onSelect(ThemePresets.customId(Color.hsv((it * 360f).coerceIn(0f, 359.9f), sat, value))) }
                )
                Text("Shade", fontSize = 12.sp, color = c.textSub)
                GradientSlider(
                    fraction = (value - 0.35f) / 0.45f,
                    brush    = Brush.horizontalGradient(listOf(Color.hsv(hue, sat, 0.35f), Color.hsv(hue, sat, 0.8f))),
                    onChange = { onSelect(ThemePresets.customId(Color.hsv(hue, sat, 0.35f + it * 0.45f))) }
                )
                OutlinedTextField(
                    value         = hexText,
                    onValueChange = { input ->
                        val clean = input.filter { ch -> ch in "0123456789abcdefABCDEF" }.take(6).uppercase()
                        hexText = clean
                        if (clean.length == 6) {
                            onSelect(ThemePresets.customId(Color(0xFF000000L or clean.toLong(16))))
                        }
                    },
                    label         = { Text("Hex color") },
                    prefix        = { Text("#") },
                    singleLine    = true,
                    supportingText = { Text("Very light colors are darkened so button text stays readable.", fontSize = 11.sp) },
                    colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.pink, unfocusedBorderColor = c.pinkLight),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color?, brush: Brush?, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(color ?: Color.Gray))
            .border(if (selected) 3.dp else 0.dp, if (selected) c.textMain else Color.Transparent, CircleShape)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun GradientSlider(fraction: Float, brush: Brush, onChange: (Float) -> Unit) {
    val c       = LocalAppColors.current
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { onChange((it.x / widthPx).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
    ) {
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(14.dp)
                .clip(RoundedCornerShape(7.dp)).background(brush)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { (fraction.coerceIn(0f, 1f) * widthPx).toDp() } - 12.dp)
                .size(24.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, c.textMain, CircleShape)
        )
    }
}

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

    val themeId    by vm.themeColor.collectAsStateWithLifecycle()
    val dayEndTime by vm.dayEndTime.collectAsStateWithLifecycle()
    var showDayEndPicker by remember { mutableStateOf(false) }

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
                listOf("Daily", "Weekly", "Reminders", "Backup", "General").forEachIndexed { i, label ->
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

                // ── General ───────────────────────────────────────────────────
                4 -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeColorCard(themeId, vm::setThemeColor)
                    Card(
                        modifier  = Modifier.fillMaxWidth().clickable { showDayEndPicker = true },
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = c.cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Day ends at", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                                Text(
                                    "When the next day starts. Anything logged before this still counts as the previous day. " +
                                        "Checking things off on another day opens the time picker at the last minute of that day.",
                                    fontSize = 12.sp, color = c.textSub
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(dayEndTime, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.pink)
                        }
                    }
                }

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

    if (showDayEndPicker) {
        TimePickerDialog("Day ends at", dayEndTime,
            onConfirm = { vm.setDayEndTime(it); showDayEndPicker = false },
            onDismiss = { showDayEndPicker = false })
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
    val c       = LocalAppColors.current
    val context = LocalContext.current
    val needsExactAlarmPermission = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !(context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: true)
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (needsExactAlarmPermission) {
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Exact alarm permission needed",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF856404)
                            )
                            Text(
                                "Android 12+ requires permission for exact alarms. Grant it so reminders fire on time.",
                                fontSize = 12.sp,
                                color    = Color(0xFF856404)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF856404))
                            ) { Text("Grant", color = Color.White, fontSize = 12.sp) }
                        }
                    }
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
            item {
                var autoOpen by remember {
                    mutableStateOf(NotificationScheduler.isAutoOpen(context) && Settings.canDrawOverlays(context))
                }
                // Re-read after returning from the system permission screen.
                val lifecycleOwner = context as androidx.lifecycle.LifecycleOwner
                DisposableEffect(lifecycleOwner) {
                    val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                        if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            autoOpen = NotificationScheduler.isAutoOpen(context) && Settings.canDrawOverlays(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(obs)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
                }
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
                        Column(Modifier.weight(1f)) {
                            Text("Open app automatically", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textMain)
                            Text("Launch the planner at reminder time. Needs \"Display over other apps\".", fontSize = 12.sp, color = c.textSub)
                        }
                        Switch(
                            checked = autoOpen,
                            onCheckedChange = { on ->
                                NotificationScheduler.setAutoOpen(context, on)
                                autoOpen = on && Settings.canDrawOverlays(context)
                                if (on && !Settings.canDrawOverlays(context)) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = c.pink)
                        )
                    }
                }
            }

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
