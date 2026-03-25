package com.dressed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dressed.app.ui.DressedApp
import com.dressed.app.ui.WardrobeViewModel
import com.dressed.app.ui.theme.DressedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DressedApplication
        setContent {
            val viewModel: WardrobeViewModel = viewModel(factory = WardrobeViewModel.factory(app))
            DressedTheme {
                DressedApp(viewModel = viewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    DressedTheme {
        androidx.compose.material3.Text("Sync project to preview full app.")
    }
}
