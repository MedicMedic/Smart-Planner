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
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marianne.smartplanner.R
import com.marianne.smartplanner.data.Goal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val KEY_SYNC = "sync_goals"

@Composable
private fun widgetColor(@ColorRes resId: Int): ColorProvider =
    ColorProvider(Color(LocalContext.current.getColor(resId)))

private val wPink: ColorProvider @Composable get() = widgetColor(R.color.widget_pink)
private val wMain: ColorProvider @Composable get() = widgetColor(R.color.widget_text_main)
private val wSub: ColorProvider @Composable get() = widgetColor(R.color.widget_text_sub)

class GoalsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry     = WidgetDataHelper.loadEntry(context)
        val goals     = entry.todayGoals
        val done      = goals.count { it.checked }
        val isSyncing = WidgetDataHelper.isSyncing(context, KEY_SYNC)
        if (isSyncing) {
            CoroutineScope(Dispatchers.IO).launch {
                delay(100)
                WidgetDataHelper.setSyncing(context, KEY_SYNC, false)
                GoalsWidget().updateAll(context)
            }
        }
        val dateLabel = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))

        provideContent {
            val openApp    = actionRunCallback<OpenAppAction>()
            val syncAction = actionRunCallback<SyncGoalsAction>()
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(R.color.widget_bg)
                    .padding(10.dp)
                    .clickable(openApp)
            ) {
                WidgetHeader(
                    title      = "Today's Goals",
                    badge      = if (goals.isEmpty()) "" else "$done/${goals.size}",
                    date       = dateLabel,
                    syncAction = syncAction,
                    isSyncing  = isSyncing
                )
                LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    if (goals.isEmpty()) {
                        item(itemId = -1L) {
                            Text(
                                text  = "No goals yet",
                                style = TextStyle(
                                    color     = wSub,
                                    fontSize  = 22.sp,
                                    fontStyle = FontStyle.Italic
                                ),
                                modifier = GlanceModifier
                                    .padding(top = 4.dp, bottom = 4.dp)
                                    .clickable(openApp)
                            )
                        }
                    } else {
                        items(goals, itemId = { it.id.toLong() }) { goal ->
                            GoalRow(goal = goal)
                        }
                    }
                    item(itemId = Long.MAX_VALUE) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(R.color.widget_pink_container)
                                .clickable(openApp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text  = "+ Add goal",
                                style = TextStyle(
                                    color      = wPink,
                                    fontSize   = 22.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal) {
    val toggleAction = actionRunCallback<ToggleGoalAction>(
        actionParametersOf(ToggleGoalAction.GOAL_ID to goal.id)
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
                if (goal.checked) R.drawable.ic_widget_cb_checked
                else              R.drawable.ic_widget_cb_unchecked
            ),
            contentDescription = null,
            modifier           = GlanceModifier.width(28.dp).height(28.dp).clickable(toggleAction)
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text     = goal.name.ifBlank { "Goal" },
            style    = TextStyle(
                color          = if (goal.checked) wSub else wMain,
                fontSize       = 22.sp,
                textDecoration = if (goal.checked) TextDecoration.LineThrough
                                 else TextDecoration.None
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )
        if (goal.checked && goal.completedAt != null) {
            Text(
                text     = goal.completedAt,
                style    = TextStyle(color = wPink, fontSize = 18.sp),
                modifier = GlanceModifier.padding(start = 4.dp)
            )
        }
    }
}

class GoalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalsWidget()
}

class ToggleGoalAction : ActionCallback {
    companion object {
        val GOAL_ID = ActionParameters.Key<Int>("goalId")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val goalId = parameters[GOAL_ID] ?: return
        WidgetDataHelper.toggleGoal(context, goalId)
        GoalsWidget().updateAll(context)
    }
}

class SyncGoalsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetDataHelper.setSyncing(context, KEY_SYNC, true)
        GoalsWidget().updateAll(context)
    }
}
