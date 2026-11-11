package com.als.presence.ui.dashboard.common

import androidx.compose.runtime.Composable
import com.als.presence.core.designsystem.DisplayWidthTier
import com.als.presence.core.designsystem.LocalScreenSizeInfo
import com.als.presence.core.designsystem.ScreenSizeInfo
import com.als.presence.core.designsystem.rememberScreenSizeInfo

// Re-export design system types so existing call sites continue to resolve seamlessly
typealias DisplayWidthTier = DisplayWidthTier
typealias ScreenSizeInfo = ScreenSizeInfo

val LocalScreenSizeInfo = LocalScreenSizeInfo

@Composable
fun rememberScreenSizeInfo(): ScreenSizeInfo = com.als.presence.core.designsystem.rememberScreenSizeInfo()
