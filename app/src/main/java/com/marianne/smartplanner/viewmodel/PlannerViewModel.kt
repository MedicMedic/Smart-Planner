package com.marianne.smartplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.marianne.smartplanner.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class AppScreen { PLANNER, SETTINGS }

enum class TimeTarget { ROUTINE, WEEKLY, GOAL }
data class TimePrompt(val target: TimeTarget, val id: Int)

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val dayRepo     = DayRepository(application)
    private val routineRepo = RoutineRepository(application)
    private val weeklyRepo  = WeeklyRepository(application)
    private val appPrefs    = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    private val dateFmt     = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFmt     = DateTimeFormatter.ofPattern("HH:mm")

    // ── Timetable name ────────────────────────────────────────────────────────

    private val _timetableName = MutableStateFlow(
        appPrefs.getString("timetable_name", "My Timetable") ?: "My Timetable"
    )
    val timetableName: StateFlow<String> = _timetableName.asStateFlow()

    fun setTimetableName(name: String) {
        _timetableName.value = name
        appPrefs.edit().putString("timetable_name", name).apply()
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private val _notificationsEnabled = MutableStateFlow(
        com.marianne.smartplanner.NotificationScheduler.isEnabled(application)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationTimes = MutableStateFlow(
        com.marianne.smartplanner.NotificationScheduler.loadTimes(application)
    )
    val notificationTimes: StateFlow<List<String>> = _notificationTimes.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        com.marianne.smartplanner.NotificationScheduler.setEnabled(getApplication(), enabled)
    }

    fun addNotificationTime(time: String) {
        val updated = _notificationTimes.value + time
        _notificationTimes.value = updated
        com.marianne.smartplanner.NotificationScheduler.schedule(getApplication(), updated)
    }

    fun removeNotificationTime(index: Int) {
        val updated = _notificationTimes.value.toMutableList().also { it.removeAt(index) }
        _notificationTimes.value = updated
        com.marianne.smartplanner.NotificationScheduler.schedule(getApplication(), updated)
    }

    fun updateNotificationTime(index: Int, time: String) {
        val updated = _notificationTimes.value.toMutableList().also { it[index] = time }
        _notificationTimes.value = updated
        com.marianne.smartplanner.NotificationScheduler.schedule(getApplication(), updated)
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    fun exportBackup(): String = com.marianne.smartplanner.data.BackupManager.exportJson(getApplication())

    fun importBackup(json: String) {
        com.marianne.smartplanner.data.BackupManager.importJson(getApplication(), json)
        _timetableName.value      = appPrefs.getString("timetable_name", "My Timetable") ?: "My Timetable"
        val newEnabled            = com.marianne.smartplanner.NotificationScheduler.isEnabled(getApplication())
        val newTimes              = com.marianne.smartplanner.NotificationScheduler.loadTimes(getApplication())
        _notificationsEnabled.value = newEnabled
        _notificationTimes.value    = newTimes
        _routine.value              = routineRepo.load()
        _weeklyTasks.value          = weeklyRepo.load()
        _entry.value                = entryFor(_currentDate.value)
        if (newEnabled) com.marianne.smartplanner.NotificationScheduler.schedule(getApplication(), newTimes)
        else            com.marianne.smartplanner.NotificationScheduler.cancelAll(getApplication())
    }

    // ── Day boundary ──────────────────────────────────────────────────────────

    private val _dayEndTime = MutableStateFlow(DayBoundary.get(application))
    val dayEndTime: StateFlow<String> = _dayEndTime.asStateFlow()

    fun setDayEndTime(time: String) {
        DayBoundary.set(getApplication(), time)
        _dayEndTime.value = time
    }

    private fun isPlannerToday() = _currentDate.value == DayBoundary.today(getApplication())

    // Checking something off on any day other than the current one can't be stamped
    // with "now", so it gets that day's last minute and the picker opens to adjust it.
    private val _timePrompt = MutableStateFlow<TimePrompt?>(null)
    val timePrompt: StateFlow<TimePrompt?> = _timePrompt.asStateFlow()

    val promptDefaultTime: String get() = DayBoundary.lastMinute(getApplication())

    fun dismissTimePrompt() { _timePrompt.value = null }

    fun confirmTimePrompt(time: String) {
        val p = _timePrompt.value ?: return
        _timePrompt.value = null
        when (p.target) {
            TimeTarget.ROUTINE -> updateRoutineTaskTime(p.id, time)
            TimeTarget.WEEKLY  -> updateWeeklyTaskTime(p.id, time)
            TimeTarget.GOAL    -> updateGoalTime(p.id, time)
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private val _screen = MutableStateFlow(AppScreen.PLANNER)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    fun openSettings()  { _screen.value = AppScreen.SETTINGS }
    fun closeSettings() { _screen.value = AppScreen.PLANNER  }

    // ── Daily routine template ────────────────────────────────────────────────

    private val _routine = MutableStateFlow(routineRepo.load())
    val routine: StateFlow<List<RoutineTaskDef>> = _routine.asStateFlow()

    fun addRoutineTask(name: String, group: String) {
        val cur = _routine.value
        val newId    = (cur.maxOfOrNull { it.id }    ?: 0) + 1
        val maxOrder = (cur.maxOfOrNull { it.order } ?: -1) + 1
        saveRoutine(cur + RoutineTaskDef(newId, name, group, maxOrder))
    }

    fun updateRoutineTaskName(id: Int, name: String) {
        saveRoutine(_routine.value.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun deleteRoutineTask(id: Int) {
        saveRoutine(_routine.value.filter { it.id != id })
    }

    fun moveRoutineTaskUp(id: Int) {
        val tasks = _routine.value.toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx <= 0) return
        val task = tasks[idx]
        val prevIdx = tasks.take(idx).indexOfLast { it.group == task.group }
        if (prevIdx < 0) return
        val prev = tasks[prevIdx]
        tasks[prevIdx] = task.copy(order = prev.order)
        tasks[idx]     = prev.copy(order = task.order)
        saveRoutine(tasks)
    }

    fun moveRoutineTaskDown(id: Int) {
        val tasks = _routine.value.toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx < 0) return
        val task       = tasks[idx]
        val nextOffset = tasks.drop(idx + 1).indexOfFirst { it.group == task.group }
        if (nextOffset < 0) return
        val nextIdx = idx + 1 + nextOffset
        val next    = tasks[nextIdx]
        tasks[idx]     = next.copy(order = task.order)
        tasks[nextIdx] = task.copy(order = next.order)
        saveRoutine(tasks)
    }

    fun addGroup(name: String) {
        if (name.isBlank() || _routine.value.any { it.group == name }) return
        addRoutineTask("New task", name)
    }

    fun renameGroup(oldName: String, newName: String) {
        if (newName.isBlank() || oldName == newName) return
        saveRoutine(_routine.value.map { if (it.group == oldName) it.copy(group = newName) else it })
    }

    fun deleteGroup(name: String) {
        saveRoutine(_routine.value.filter { it.group != name })
    }

    private fun saveRoutine(tasks: List<RoutineTaskDef>) {
        _routine.value = tasks
        routineRepo.save(tasks)
    }

    // ── Weekly routine template ───────────────────────────────────────────────

    private val _weeklyTasks = MutableStateFlow(weeklyRepo.load())
    val weeklyTasks: StateFlow<List<WeeklyTaskDef>> = _weeklyTasks.asStateFlow()

    fun addWeeklyTask(name: String, group: String, days: List<Int>) {
        val cur = _weeklyTasks.value
        val newId    = (cur.maxOfOrNull { it.id }    ?: 0) + 1
        val maxOrder = (cur.maxOfOrNull { it.order } ?: -1) + 1
        saveWeekly(cur + WeeklyTaskDef(newId, name, group, maxOrder, days))
    }

    fun updateWeeklyTaskName(id: Int, name: String) {
        saveWeekly(_weeklyTasks.value.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun deleteWeeklyTask(id: Int) {
        saveWeekly(_weeklyTasks.value.filter { it.id != id })
    }

    fun moveWeeklyTaskUp(id: Int) {
        val tasks = _weeklyTasks.value.toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx <= 0) return
        val task    = tasks[idx]
        val prevIdx = tasks.take(idx).indexOfLast { it.group == task.group }
        if (prevIdx < 0) return
        val prev = tasks[prevIdx]
        tasks[prevIdx] = task.copy(order = prev.order)
        tasks[idx]     = prev.copy(order = task.order)
        saveWeekly(tasks)
    }

    fun moveWeeklyTaskDown(id: Int) {
        val tasks = _weeklyTasks.value.toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx < 0) return
        val task       = tasks[idx]
        val nextOffset = tasks.drop(idx + 1).indexOfFirst { it.group == task.group }
        if (nextOffset < 0) return
        val nextIdx = idx + 1 + nextOffset
        val next    = tasks[nextIdx]
        tasks[idx]     = next.copy(order = task.order)
        tasks[nextIdx] = task.copy(order = next.order)
        saveWeekly(tasks)
    }

    fun addWeeklyGroup(name: String, days: List<Int>) {
        if (name.isBlank() || _weeklyTasks.value.any { it.group == name }) return
        addWeeklyTask("New task", name, days)
    }

    fun renameWeeklyGroup(oldName: String, newName: String) {
        if (newName.isBlank() || oldName == newName) return
        saveWeekly(_weeklyTasks.value.map { if (it.group == oldName) it.copy(group = newName) else it })
    }

    fun deleteWeeklyGroup(name: String) {
        saveWeekly(_weeklyTasks.value.filter { it.group != name })
    }

    fun setWeeklyGroupDays(group: String, days: List<Int>) {
        saveWeekly(_weeklyTasks.value.map { if (it.group == group) it.copy(days = days) else it })
    }

    private fun saveWeekly(tasks: List<WeeklyTaskDef>) {
        _weeklyTasks.value = tasks
        weeklyRepo.save(tasks)
    }

    // ── Date navigation ───────────────────────────────────────────────────────

    private val _currentDate = MutableStateFlow(DayBoundary.today(application))
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    private val _entry = MutableStateFlow(entryFor(DayBoundary.today(application)))
    val entry: StateFlow<DayEntry> = _entry.asStateFlow()

    fun goToDate(date: LocalDate) {
        _currentDate.value = date
        _entry.value = entryFor(date)
    }

    fun previousDay() = goToDate(_currentDate.value.minusDays(1))
    fun nextDay()     = goToDate(_currentDate.value.plusDays(1))

    // ── Awake range ───────────────────────────────────────────────────────────

    fun setAwakeStart(t: String)       = update { it.copy(awakeStart = t) }
    fun setAwakeEnd(t: String)         = update { it.copy(awakeEnd = t) }
    fun setAwakeEndNextDay(v: Boolean) = update { it.copy(awakeEndNextDay = v) }

    // ── Routine checks ────────────────────────────────────────────────────────

    fun toggleRoutineTask(taskId: Int) {
        update(updateChecklist = true) { entry ->
            val checks = entry.routineChecks.toMutableMap()
            val times  = entry.routineCheckTimes.toMutableMap()
            val checked = !(checks[taskId] ?: false)
            checks[taskId] = checked
            if (checked) times[taskId] = stampTime() else times.remove(taskId)
            entry.copy(routineChecks = checks, routineCheckTimes = times)
        }
        promptIfChecked(TimeTarget.ROUTINE, taskId) { it.isRoutineTaskChecked(taskId) }
    }

    // ── Weekly checks ─────────────────────────────────────────────────────────

    fun toggleWeeklyTask(taskId: Int) {
        update { entry ->
            val checks = entry.weeklyChecks.toMutableMap()
            val times  = entry.weeklyCheckTimes.toMutableMap()
            val checked = !(checks[taskId] ?: false)
            checks[taskId] = checked
            if (checked) times[taskId] = stampTime() else times.remove(taskId)
            entry.copy(weeklyChecks = checks, weeklyCheckTimes = times)
        }
        promptIfChecked(TimeTarget.WEEKLY, taskId) { it.isWeeklyTaskChecked(taskId) }
    }

    // ── Today's goals ─────────────────────────────────────────────────────────

    fun addGoal() {
        val newId = (_entry.value.todayGoals.maxOfOrNull { it.id } ?: 0) + 1
        update { it.copy(todayGoals = it.todayGoals + Goal(newId, "")) }
    }

    fun updateGoalName(id: Int, name: String) = update {
        it.copy(todayGoals = it.todayGoals.map { g -> if (g.id == id) g.copy(name = name) else g })
    }

    fun toggleGoal(id: Int) {
        update(updateGoals = true) {
            it.copy(todayGoals = it.todayGoals.map { g ->
                if (g.id == id) g.copy(
                    checked     = !g.checked,
                    completedAt = if (!g.checked) stampTime() else null
                ) else g
            })
        }
        promptIfChecked(TimeTarget.GOAL, id) { e -> e.todayGoals.any { it.id == id && it.checked } }
    }

    fun deleteGoal(id: Int) = update(updateGoals = true) {
        it.copy(todayGoals = it.todayGoals.filter { g -> g.id != id })
    }

    // ── Time edits ────────────────────────────────────────────────────────────

    fun updateRoutineTaskTime(taskId: Int, time: String) = update(updateChecklist = true) { entry ->
        entry.copy(routineCheckTimes = entry.routineCheckTimes.toMutableMap().also { it[taskId] = time })
    }

    fun updateWeeklyTaskTime(taskId: Int, time: String) = update { entry ->
        entry.copy(weeklyCheckTimes = entry.weeklyCheckTimes.toMutableMap().also { it[taskId] = time })
    }

    fun updateGoalTime(goalId: Int, time: String) = update(updateGoals = true) {
        it.copy(todayGoals = it.todayGoals.map { g -> if (g.id == goalId) g.copy(completedAt = time) else g })
    }

    // ── Diary + Mood ──────────────────────────────────────────────────────────

    fun setDiary(text: String) = update { it.copy(diary = text) }
    fun setMood(ordinal: Int)  = update { it.copy(moodOrdinal = ordinal) }

    // ── Internal ──────────────────────────────────────────────────────────────

    fun refreshEntry() {
        _entry.value = entryFor(_currentDate.value)
    }

    private fun entryFor(date: LocalDate): DayEntry =
        dayRepo.load(date.format(dateFmt)) ?: DayEntry(date = date.format(dateFmt))

    private fun update(
        updateChecklist: Boolean = false,
        updateGoals: Boolean = false,
        transform: (DayEntry) -> DayEntry
    ) {
        _entry.value = transform(_entry.value)
        dayRepo.save(_entry.value, updateChecklist, updateGoals)
    }

    private fun nowTime(): String = LocalTime.now().format(timeFmt)

    private fun stampTime(): String =
        if (isPlannerToday()) nowTime() else DayBoundary.lastMinute(getApplication())

    private fun promptIfChecked(target: TimeTarget, id: Int, isChecked: (DayEntry) -> Boolean) {
        if (!isPlannerToday() && isChecked(_entry.value)) _timePrompt.value = TimePrompt(target, id)
    }
}
