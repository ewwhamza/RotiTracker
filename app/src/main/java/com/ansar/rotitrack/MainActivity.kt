package com.ansar.rotitrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ansar.rotitrack.ui.RotiAppShell
import com.ansar.rotitrack.ui.RotiViewModel
import com.ansar.rotitrack.ui.theme.RotiTrackTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RotiViewModel by viewModels { RotiViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RotiTrackTheme {
                RotiAppShell(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the app was left open overnight, the home screen should move on to the new day.
        viewModel.refreshToday()
    }
}
