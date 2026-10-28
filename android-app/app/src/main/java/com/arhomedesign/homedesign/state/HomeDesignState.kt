package com.arhomedesign.homedesign.state

import androidx.compose.ui.graphics.Color
import com.arhomedesign.homedesign.brand.FinishType
import com.arhomedesign.paintpreview.PaintSurfaceFilter

enum class ArTrackingStatus { TRACKING, LIMITED, NONE }

data class HomeDesignState(
    val selectedColor: Color = Color(0xFF7EB6D9),
    val selectedColorHex: String = "7EB6D9",
    val tintOpacity: Float = 0.42f,
    val selectedFilter: PaintSurfaceFilter = PaintSurfaceFilter.ALL,
    val wallSensitivity: Float = 0.50f,
    val showOutlines: Boolean = true,
    val advancedExpanded: Boolean = false,
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.NONE,
    val planeCount: Int = 0,
    val activeModeLabel: String = "AR Preview",
    val selectedBrandName: String? = null,
    val selectedProductName: String? = null,
    val selectedProductCode: String? = null,
    val finishType: FinishType = FinishType.SATIN,
    val coachingVisible: Boolean = true,
)
