package com.arhomedesign.homedesign.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.arhomedesign.core.ui.theme.ArHomeDesignTheme
import com.arhomedesign.auth.data.AuthRepository
import com.arhomedesign.auth.ui.AuthScreen
import com.arhomedesign.auth.ui.AuthViewModel
import com.arhomedesign.core.di.FirebaseProviders
import com.arhomedesign.homedesign.HomeDesignViewModel
import com.arhomedesign.homedesign.ui.OnboardingScreen
import com.arhomedesign.homedesign.state.UiState
import com.arhomedesign.homedesign.ui.PaintBrandScreen
import com.arhomedesign.homedesign.ui.PhotoRecolorScreen
import com.arhomedesign.homedesign.ui.SavedPalettesScreen
import com.arhomedesign.paintpreview.ui.PaintPreviewScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat

private enum class HomeDestination(val route: String, val label: String) {
    AR("ar_preview", "AR Preview"),
    PHOTO("photo_recolor", "Photo Recolor"),
    PALETTES("saved_palettes", "Saved");

    companion object {
        const val BRAND_PICKER_ROUTE = "brand_picker"
        const val ONBOARD_ROUTE = "onboarding"
    }
}

@Composable
fun HomeDesignApp(
    showOnboarding: Boolean = false,
    crashLog: String? = null,
    onCrashShared: () -> Unit = {},
    viewModel: HomeDesignViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val seed = (state as? UiState.Success)?.data?.selectedColor
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val tabs = HomeDestination.entries
    val snackbars = remember { SnackbarHostState() }
    val arRoute = "${HomeDestination.AR.route}?color={color}"
    val authViewModel = remember { AuthViewModel(AuthRepository(FirebaseProviders.auth)) }
    val authState by authViewModel.uiState.collectAsState()
    var guestMode by rememberSaveable { mutableStateOf(false) }

    ArHomeDesignTheme(seedColor = seed) {
        if (!authState.isLoggedIn && !guestMode) {
            AuthScreen(
                state = authState,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onClearError = authViewModel::clearError,
                onContinueAsGuest = { guestMode = true },
            )
            return@ArHomeDesignTheme
        }
        LaunchedEffect(crashLog) {
            if (crashLog.isNullOrBlank()) return@LaunchedEffect
            val result = snackbars.showSnackbar("Something went wrong - tap to report", actionLabel = "Report")
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                ShareCompat.IntentBuilder(context)
                    .setType("text/plain")
                    .setText(crashLog)
                    .setChooserTitle("Share crash log")
                    .startChooser()
            }
            onCrashShared()
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbars) },
            bottomBar = {
                if (currentRoute != HomeDestination.ONBOARD_ROUTE) {
                    NavigationBar {
                        tabs.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute?.startsWith(destination.route) == true,
                                onClick = {
                                    val target = if (destination == HomeDestination.AR) {
                                        "${destination.route}?color="
                                    } else {
                                        destination.route
                                    }
                                    navController.navigate(target) {
                                        popUpTo(HomeDestination.AR.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                label = { Text(destination.label) },
                                icon = {
                                    val icon = when (destination) {
                                        HomeDestination.AR -> Icons.Rounded.ViewInAr
                                        HomeDestination.PHOTO -> Icons.Rounded.AutoAwesome
                                        HomeDestination.PALETTES -> Icons.Rounded.Collections
                                    }
                                    Icon(imageVector = icon, contentDescription = destination.label)
                                }
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = if (showOnboarding) HomeDestination.ONBOARD_ROUTE else HomeDestination.AR.route,
                modifier = Modifier.padding(paddingValues),
            ) {
                composable(
                    route = arRoute,
                    arguments = listOf(navArgument("color") { type = NavType.StringType; defaultValue = "" }),
                    deepLinks = listOf(navDeepLink { uriPattern = "arhome://arpreview?color={color}" }),
                ) { backStackEntry ->
                    val colorHex = backStackEntry.arguments?.getString("color").orEmpty()
                    if (colorHex.length == 6) {
                        LaunchedEffect(colorHex) { viewModel.setHexAndColor(colorHex) }
                    }
                    PaintPreviewScreen(
                        viewModel = viewModel,
                        onOpenBrandPicker = { navController.navigate(HomeDestination.BRAND_PICKER_ROUTE) },
                        onOpenPhotoRecolor = { navController.navigate(HomeDestination.PHOTO.route) },
                    )
                }
                composable(HomeDestination.PHOTO.route) {
                    PhotoRecolorScreen()
                }
                composable(HomeDestination.PALETTES.route) {
                    SavedPalettesScreen(
                        viewModel = viewModel,
                        onTryAr = { hex ->
                            navController.navigate("${HomeDestination.AR.route}?color=$hex")
                        },
                    )
                }
                composable(HomeDestination.BRAND_PICKER_ROUTE) {
                    PaintBrandScreen(
                        viewModel = viewModel,
                        onBackToAr = { navController.popBackStack() },
                    )
                }
                composable(HomeDestination.ONBOARD_ROUTE) {
                    OnboardingScreen {
                        viewModel.completeOnboarding()
                        navController.navigate(HomeDestination.AR.route) {
                            popUpTo(HomeDestination.ONBOARD_ROUTE) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}
