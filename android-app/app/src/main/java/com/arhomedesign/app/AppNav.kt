package com.arhomedesign.app

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arhomedesign.ar.ui.ARScanScreen
import com.arhomedesign.ar.ui.ARScanViewModel
import com.arhomedesign.auth.ui.AuthScreen
import com.arhomedesign.auth.ui.AuthViewModel
import com.arhomedesign.projects.ui.CreateProjectScreen
import com.arhomedesign.paintpreview.ui.PaintPreviewScreen
import com.arhomedesign.projects.ui.ProjectsScreen
import com.arhomedesign.projects.ui.ProjectsViewModel

private const val ROUTE_AUTH = "auth"
private const val ROUTE_PROJECTS = "projects"
private const val ROUTE_CREATE_PROJECT = "create_project"
private const val ROUTE_AR_SCAN = "ar_scan/{projectId}"
private const val ROUTE_PAINT_PREVIEW = "paint_preview"

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel,
    projectsViewModel: ProjectsViewModel,
    arScanViewModel: ARScanViewModel,
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.uiState.collectAsState()
    val projectsState by projectsViewModel.uiState.collectAsState()
    val arState by arScanViewModel.uiState.collectAsState()

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(ROUTE_PROJECTS) {
                popUpTo(ROUTE_AUTH) { inclusive = true }
            }
            projectsViewModel.loadProjects()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.isLoggedIn) ROUTE_PROJECTS else ROUTE_AUTH
    ) {
        composable(ROUTE_AUTH) {
            AuthScreen(
                state = authState,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onClearError = authViewModel::clearError
            )
        }
        composable(ROUTE_PROJECTS) {
            ProjectsScreen(
                state = projectsState,
                onRefresh = projectsViewModel::loadProjects,
                onCreateProjectClick = { navController.navigate(ROUTE_CREATE_PROJECT) },
                onProjectClick = { project ->
                    navController.navigate("ar_scan/${project.id}")
                },
                onOpenPaintPreview = { navController.navigate(ROUTE_PAINT_PREVIEW) }
            )
        }
        composable(ROUTE_PAINT_PREVIEW) {
            PaintPreviewScreen(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_CREATE_PROJECT) {
            CreateProjectScreen(
                onBack = { navController.popBackStack() },
                onCreateProject = { title, description ->
                    projectsViewModel.createProject(title, description)
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = ROUTE_AR_SCAN,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
            val context = LocalContext.current
            LaunchedEffect(projectId) {
                if (projectId.isNotBlank()) {
                    arScanViewModel.loadScans(projectId)
                }
                arScanViewModel.refreshArCoreStatus(context)
            }
            ARScanScreen(
                context = context,
                projectId = projectId,
                onBack = { navController.popBackStack() },
                state = arState,
                onRefreshArCoreStatus = { arScanViewModel.refreshArCoreStatus(context) },
                onInstallArCore = {
                    (context as? Activity)?.let { arScanViewModel.requestArCoreInstall(it) }
                },
                onStartArSession = { ctx ->
                    arScanViewModel.startArSession(ctx)
                },
                onUpdateDetectedPlanes = {
                    arScanViewModel.updateDetectedPlanes()
                },
                onCaptureFromArCore = { roomName, widthMeters, lengthMeters, heightMeters ->
                    if (projectId.isNotBlank()) {
                        arScanViewModel.saveScan(
                            projectId = projectId,
                            roomName = roomName,
                            widthMeters = widthMeters,
                            lengthMeters = lengthMeters,
                            heightMeters = heightMeters
                        )
                    }
                },
                onSelectActiveRoom = { roomId ->
                    arScanViewModel.selectActiveRoom(roomId)
                    if (projectId.isNotBlank()) arScanViewModel.loadScans(projectId)
                },
                onSelectSurfaceType = { surfaceType ->
                    arScanViewModel.selectSurfaceType(surfaceType)
                },
                onSelectMaterialPreset = { materialPreset ->
                    arScanViewModel.selectMaterialPreset(materialPreset)
                },
                onSetColorHex = { colorHex ->
                    arScanViewModel.setColorHex(colorHex)
                },
                onAddOverlayPrimitive = {
                    if (projectId.isNotBlank()) {
                        arScanViewModel.addOverlayPrimitive(projectId)
                    }
                },
                onSaveDesignVersion = { notes ->
                    if (projectId.isNotBlank()) {
                        arScanViewModel.saveDesignVersion(projectId, notes)
                    }
                },
                onGenerateEstimate = {
                    if (projectId.isNotBlank()) {
                        arScanViewModel.generateEstimate(projectId)
                    }
                },
                onCompareVersions = {
                    arScanViewModel.compareLatestVersions()
                },
                onBuildExportSummary = {
                    if (projectId.isNotBlank()) arScanViewModel.buildExportSummary(projectId)
                },
                onCreateShareLink = {
                    if (projectId.isNotBlank()) arScanViewModel.createShareLink(projectId)
                },
                onBuildSharePackage = {
                    if (projectId.isNotBlank()) arScanViewModel.buildSharePackage(projectId)
                },
                onToggleMaterialPreview = { overlayId ->
                    arScanViewModel.toggleMaterialPreview(overlayId)
                },
                onApplyMaterialToPreviewed = {
                    if (projectId.isNotBlank()) arScanViewModel.applyMaterialToPreviewedOverlays(projectId)
                },
                onClearPreviewSelection = {
                    arScanViewModel.clearMaterialPreviewSelection()
                },
                onPersistOverlayMaterial = { overlayId ->
                    if (projectId.isNotBlank()) {
                        arScanViewModel.persistOverlayMaterial(projectId, overlayId)
                    }
                },
                onAdjustOverlayTransform = { overlayId, dx, dy, dz, scale, rot ->
                    if (projectId.isNotBlank()) {
                        arScanViewModel.updateOverlayTransform(
                            projectId = projectId,
                            overlayId = overlayId,
                            deltaX = dx,
                            deltaY = dy,
                            deltaZ = dz,
                            scaleDelta = scale,
                            rotationDelta = rot
                        )
                    }
                },
                onUndoOverlayEdits = {
                    if (projectId.isNotBlank()) arScanViewModel.undoOverlayEdits(projectId)
                },
                onRedoOverlayEdits = {
                    if (projectId.isNotBlank()) arScanViewModel.redoOverlayEdits(projectId)
                },
                onStopArSession = {
                    arScanViewModel.stopArSession()
                },
                onRefresh = { if (projectId.isNotBlank()) arScanViewModel.loadScans(projectId) }
            )
        }
    }
}
