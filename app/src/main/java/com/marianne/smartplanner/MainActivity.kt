package com.marianne.smartplanner

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import com.marianne.smartplanner.ui.PlannerScreen
import com.marianne.smartplanner.ui.SettingsScreen
import com.marianne.smartplanner.ui.theme.SmartPlannerTheme
import com.marianne.smartplanner.viewmodel.AppScreen
import com.marianne.smartplanner.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var vm: PlannerViewModel
    private var askedFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlannerTheme {
                vm = viewModel()
                val screen by vm.screen.collectAsStateWithLifecycle()
                when (screen) {
                    AppScreen.PLANNER  -> PlannerScreen(vm)
                    AppScreen.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Android 14+ requires the user to grant full-screen notifications manually.
        if (!askedFullScreen &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            NotificationScheduler.isEnabled(this) &&
            !getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            askedFullScreen = true
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
            )
        }
        if (::vm.isInitialized) vm.refreshEntry()
    }
}
