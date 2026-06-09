package com.marianne.smartplanner.widget

import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marianne.smartplanner.R
import com.marianne.smartplanner.data.RoutineTaskDef
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val KEY_SYNC = "sync_checklist"

@Composable
private fun widgetColor(@ColorRes resId: Int): ColorProvider =
    ColorProvider(Color(LocalContext.current.getColor(resId)))

private val wPink: ColorProvider @Composable get() = widgetColor(R.color.widget_pink)
private val wMain: ColorProvider @Composable get() = widgetColor(R.color.widget_text_main)
private val wSub: ColorProvider @Composable get() = widgetColor(R.color.widget_text_sub)

class ChecklistWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry     = WidgetDataHelper.loadEntry(context)
        val routine   = WidgetDataHelper.loadRoutine(context)
        val groups    = routine.sortedBy { it.order }.map { it.group }.distinct()
        val grouped   = routine.groupBy { it.group }
        val done      = routine.count { entry.isRoutineTaskChecked(it.id) }
        val isSyncing = WidgetDataHelper.isSyncing(context, KEY_SYNC)
        if (isSyncing) {
            CoroutineScope(Dispatchers.IO).launch {
                delay(100)
                WidgetDataHelper.setSyncing(context, KEY_SYNC, false)
                ChecklistWidget().updateAll(context)
            }
        }
        val dateLabel = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))

        provideContent {
            val openApp    = actionRunCallback<OpenAppAction>()
            val syncAction = actionRunCallback<SyncChecklistAction>()
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(R.color.widget_bg)
                    .padding(10.dp)
                    .clickable(openApp)
            ) {
                WidgetHeader(
                    title      = "Daily Routine",
                    badge      = "$done/${routine.size}",
                    date       = dateLabel,
                    syncAction = syncAction,
                    isSyncing  = isSyncing
                )
                LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    groups.forEachIndexed { groupIdx, group ->
                        val tasks = (grouped[group] ?: emptyList()).sortedBy { it.order }
                        if (tasks.isNotEmpty()) {
                            item(itemId = -(groupIdx + 1L)) {
                                Text(
                                    text     = group.uppercase(),
                                    style    = TextStyle(
                                        color      = wPink,
                                        fontSize   = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 1.dp)
                                        .clickable(openApp)
                                )
                            }
                            items(tasks, itemId = { it.id.toLong() }) { task ->
                                ChecklistTaskRow(
                                    task    = task,
                                    checked = entry.isRoutineTaskChecked(task.id),
                                    time    = entry.routineCheckTimes[task.id]
                                        .takeIf { entry.isRoutineTaskChecked(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistTaskRow(task: RoutineTaskDef, checked: Boolean, time: String?) {
    val toggleAction = actionRunCallback<ToggleRoutineTaskAction>(
        actionParametersOf(ToggleRoutineTaskAction.TASK_ID to task.id)
    )
    val openApp = actionRunCallback<OpenAppAction>()
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(openApp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider           = ImageProvider(
                if (checked) R.drawable.ic_widget_cb_checked
                else         R.drawable.ic_widget_cb_unchecked
            ),
            contentDescription = null,
            modifier           = GlanceModifier.width(28.dp).height(28.dp).clickable(toggleAction)
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text     = task.name,
            style    = TextStyle(
                color          = if (checked) wSub else wMain,
                fontSize       = 22.sp,
                textDecoration = if (checked) TextDecoration.LineThrough
                                 else TextDecoration.None
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )
        if (checked && time != null) {
            Text(
                text     = time,
                style    = TextStyle(color = wPink, fontSize = 18.sp),
                modifier = GlanceModifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
internal fun WidgetHeader(
    title: String,
    badge: String,
    date: String,
    syncAction: Action,
    isSyncing: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val openApp = actionRunCallback<OpenAppAction>()
    Row(
        modifier          = modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = title,
            style    = TextStyle(color = wPink, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight().clickable(openApp)
        )
        Text(
            text     = listOf(badge, date).filter { it.isNotEmpty() }.joinToString("  "),
            style    = TextStyle(color = wPink, fontSize = 20.sp),
            modifier = GlanceModifier.clickable(openApp)
        )
        Spacer(GlanceModifier.width(8.dp))
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(20.dp),
                color    = wPink
            )
        } else {
            Text(
                text     = "↻",
                style    = TextStyle(color = wPink, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(syncAction)
            )
        }
    }
}

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistWidget()
}

class ToggleRoutineTaskAction : ActionCallback {
    companion object {
        val TASK_ID = ActionParameters.Key<Int>("taskId")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID] ?: return
        WidgetDataHelper.toggleRoutineTask(context, taskId)
        ChecklistWidget().updateAll(context)
    }
}

class SyncChecklistAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetDataHelper.setSyncing(context, KEY_SYNC, true)
        ChecklistWidget().updateAll(context)
    }
}
