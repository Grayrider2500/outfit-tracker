package com.dressed.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dressed.app.ui.home.LandingScreen
import com.dressed.app.ui.library.LibrariesNav
import com.dressed.app.ui.outfits.OutfitsNav
import com.dressed.app.ui.outfits.OutfitsViewModel
import com.dressed.app.ui.picker.PickerScreen
import com.dressed.app.ui.picker.PickerViewModel
import com.dressed.app.ui.wardrobe.WardrobeNav
import com.dressed.app.ui.wardrobe.WardrobeSearchNav

private const val ROUTE_LANDING = "landing"
private const val ROUTE_WARDROBE = "wardrobe"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_OUTFITS = "outfits"
private const val ROUTE_PICKER = "picker"
private const val ROUTE_LIBRARIES = "libraries"

@Composable
fun DressedApp(
    viewModel: WardrobeViewModel,
    outfitsViewModel: OutfitsViewModel,
    pickerViewModel: PickerViewModel,
    librariesViewModel: LibrariesViewModel,
    pendingLibraryImport: MutableState<Uri?>,
) {
    val rootNav = rememberNavController()
    val context = LocalContext.current

    val uriToImport = pendingLibraryImport.value
    LaunchedEffect(uriToImport) {
        val uri = pendingLibraryImport.value ?: return@LaunchedEffect
        pendingLibraryImport.value = null
        librariesViewModel.importLibraryFromUri(uri) { err ->
            if (err != null) {
                Toast.makeText(context, "Library import failed: $err", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Library imported", Toast.LENGTH_LONG).show()
                rootNav.navigate(ROUTE_LIBRARIES) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = rootNav,
        startDestination = ROUTE_LANDING,
    ) {
        composable(ROUTE_LANDING) {
            LandingScreen(
                viewModel = viewModel,
                librariesViewModel = librariesViewModel,
                onMyWardrobe = { rootNav.navigate(ROUTE_WARDROBE) },
                onSearchFilter = { rootNav.navigate(ROUTE_SEARCH) },
                onOutfits = { rootNav.navigate(ROUTE_OUTFITS) },
                onSuggestOutfits = { rootNav.navigate(ROUTE_PICKER) },
                onLibraries = { rootNav.navigate(ROUTE_LIBRARIES) },
                onNavigateToBorrowedLibraries = {
                    rootNav.navigate(ROUTE_LIBRARIES) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_WARDROBE) {
            WardrobeNav(
                viewModel = viewModel,
                onNavigateHome = {
                    rootNav.popBackStack(ROUTE_LANDING, inclusive = false)
                },
            )
        }
        composable(ROUTE_SEARCH) {
            WardrobeSearchNav(
                viewModel = viewModel,
                onNavigateHome = {
                    rootNav.popBackStack(ROUTE_LANDING, inclusive = false)
                },
            )
        }
        composable(ROUTE_OUTFITS) {
            OutfitsNav(
                wardrobeViewModel = viewModel,
                outfitsViewModel = outfitsViewModel,
                onNavigateHome = {
                    rootNav.popBackStack(ROUTE_LANDING, inclusive = false)
                },
            )
        }
        composable(ROUTE_PICKER) {
            PickerScreen(
                viewModel = pickerViewModel,
                onNavigateHome = {
                    rootNav.popBackStack(ROUTE_LANDING, inclusive = false)
                },
            )
        }
        composable(ROUTE_LIBRARIES) {
            LibrariesNav(
                viewModel = librariesViewModel,
                onNavigateHome = {
                    rootNav.popBackStack(ROUTE_LANDING, inclusive = false)
                },
            )
        }
    }
}
