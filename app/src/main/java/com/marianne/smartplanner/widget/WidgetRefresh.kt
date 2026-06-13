package com.marianne.smartplanner.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reliable widget refresh.
 *
 * The naive approach — write SharedPreferences then updateAll() — is unreliable on
 * rapid taps: provideGlance reads the data once when its session starts, and Glance
 * coalesces rapid session restarts, so the final render can be dropped and the
 * widget sticks on a stale state (checks that don't show until Sync).
 *
 * Fix: the widgets read their data INSIDE the composition and depend on [REV_KEY],
 * a revision counter kept in Glance's own managed state. [bumpChecklist]/[bumpGoals]
 * increment that counter via updateAppWidgetState() — a change to observed state that
 * Glance reliably recomposes for — so every toggle re-reads SharedPreferences and
 * repaints the final state, even after a fast burst.
 *
 * A per-widget [Mutex] serializes updateAll so two never overlap ("SessionWorker
 * attempted restart but Session is not available").
 */
object WidgetRefresher {
    val REV_KEY = longPreferencesKey("widget_rev")

    private val checklistMutex = Mutex()
    private val goalsMutex = Mutex()

    /** Widget-side toggle (has a GlanceId): bump observed revision, then update. */
    suspend fun bumpChecklist(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { it[REV_KEY] = (it[REV_KEY] ?: 0L) + 1 }
        checklistMutex.withLock { ChecklistWidget().updateAll(context) }
    }

    suspend fun bumpGoals(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { it[REV_KEY] = (it[REV_KEY] ?: 0L) + 1 }
        goalsMutex.withLock { GoalsWidget().updateAll(context) }
    }

    /** App-side save (no GlanceId): restart the session so the composition re-reads. */
    suspend fun refreshChecklist(context: Context) {
        checklistMutex.withLock { ChecklistWidget().updateAll(context) }
    }

    suspend fun refreshGoals(context: Context) {
        goalsMutex.withLock { GoalsWidget().updateAll(context) }
    }
}
