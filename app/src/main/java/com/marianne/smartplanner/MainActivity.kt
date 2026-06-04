package com.marianne.smartplanner

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlannerTheme {
                val vm: PlannerViewModel = viewModel()
                val screen by vm.screen.collectAsStateWithLifecycle()
                when (screen) {
                    AppScreen.PLANNER  -> PlannerScreen(vm)
                    AppScreen.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }
}
