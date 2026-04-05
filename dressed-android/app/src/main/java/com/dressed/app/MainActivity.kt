package com.dressed.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dressed.app.ui.DressedApp
import com.dressed.app.ui.LibrariesViewModel
import com.dressed.app.ui.WardrobeViewModel
import com.dressed.app.ui.outfits.OutfitsViewModel
import com.dressed.app.ui.picker.PickerViewModel
import com.dressed.app.ui.theme.DressedTheme

class MainActivity : ComponentActivity() {

    private val pendingLibraryImport: MutableState<Uri?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureLibraryIntent(intent)
        val app = application as DressedApplication
        setContent {
            val wardrobeViewModel: WardrobeViewModel = viewModel(factory = WardrobeViewModel.factory(app))
            val outfitsViewModel: OutfitsViewModel = viewModel(factory = OutfitsViewModel.factory(app))
            val pickerViewModel: PickerViewModel = viewModel(factory = PickerViewModel.factory(app))
            val librariesViewModel: LibrariesViewModel = viewModel(factory = LibrariesViewModel.factory(app))
            DressedTheme {
                DressedApp(
                    viewModel = wardrobeViewModel,
                    outfitsViewModel = outfitsViewModel,
                    pickerViewModel = pickerViewModel,
                    librariesViewModel = librariesViewModel,
                    pendingLibraryImport = pendingLibraryImport,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureLibraryIntent(intent)
    }

    private fun captureLibraryIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val data: Uri = intent.data ?: return
        val path = (data.path ?: data.lastPathSegment).orEmpty()
        val looksLikeLibrary = path.endsWith(".dressed-library", ignoreCase = true) ||
            data.toString().contains(".dressed-library", ignoreCase = true)
        if (looksLikeLibrary) {
            pendingLibraryImport.value = data
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
