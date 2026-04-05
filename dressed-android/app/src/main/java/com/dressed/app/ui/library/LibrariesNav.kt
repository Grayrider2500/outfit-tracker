package com.dressed.app.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.ui.LibrariesViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val ROUTE_LIST = "libraries_list"
private const val ROUTE_DETAIL = "libraries_detail/{id}/{title}"

@Composable
fun LibrariesNav(
    viewModel: LibrariesViewModel,
    onNavigateHome: () -> Unit,
) {
    val navController = rememberNavController()
    val pendingImportNav by viewModel.pendingOpenImportedLibrary.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(pendingImportNav) {
        val pair = pendingImportNav ?: return@LaunchedEffect
        val (libId, sharer) = pair
        viewModel.consumePendingOpenImportedLibrary()
        val enc = URLEncoder.encode(sharer, StandardCharsets.UTF_8.toString())
        navController.navigate("libraries_detail/$libId/$enc") {
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            LibrariesListScreen(
                viewModel = viewModel,
                onNavigateHome = onNavigateHome,
                onLibraryClick = { id, title ->
                    val enc = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                    navController.navigate("libraries_detail/$id/$enc")
                },
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val titleEnc = entry.arguments?.getString("title").orEmpty()
            val sharerName = java.net.URLDecoder.decode(titleEnc, StandardCharsets.UTF_8.toString())
            val displayTitle = "$sharerName's Library"
            BorrowedLibraryDetailScreen(
                libraryId = id,
                libraryTitle = displayTitle,
                sharerName = sharerName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
