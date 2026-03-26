package com.dressed.app.ui.wardrobe

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dressed.app.ui.WardrobeViewModel

private const val ROUTE_SEARCH = "search_home"
private const val ROUTE_ADD = "search_add"
private const val ROUTE_DETAIL = "search_detail/{id}"

@Composable
fun WardrobeSearchNav(viewModel: WardrobeViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_SEARCH,
    ) {
        composable(ROUTE_SEARCH) {
            WardrobeSearchScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate(ROUTE_ADD) },
                onItemClick = { id -> navController.navigate("search_detail/$id") },
            )
        }
        composable(ROUTE_ADD) {
            AddItemScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ItemDetailScreen(
                itemId = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
