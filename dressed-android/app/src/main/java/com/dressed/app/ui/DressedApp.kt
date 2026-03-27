package com.dressed.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dressed.app.ui.home.LandingScreen
import com.dressed.app.ui.outfits.OutfitsNav
import com.dressed.app.ui.outfits.OutfitsViewModel
import com.dressed.app.ui.wardrobe.WardrobeNav
import com.dressed.app.ui.wardrobe.WardrobeSearchNav

private const val ROUTE_LANDING = "landing"
private const val ROUTE_WARDROBE = "wardrobe"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_OUTFITS = "outfits"

@Composable
fun DressedApp(viewModel: WardrobeViewModel, outfitsViewModel: OutfitsViewModel) {
    val rootNav = rememberNavController()

    NavHost(
        navController = rootNav,
        startDestination = ROUTE_LANDING,
    ) {
        composable(ROUTE_LANDING) {
            LandingScreen(
                viewModel = viewModel,
                onMyWardrobe = { rootNav.navigate(ROUTE_WARDROBE) },
                onSearchFilter = { rootNav.navigate(ROUTE_SEARCH) },
                onOutfits = { rootNav.navigate(ROUTE_OUTFITS) },
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
    }
}
