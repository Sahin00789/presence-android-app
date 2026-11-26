package com.als.presence.ui.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.als.presence.core.designsystem.PresenceDesign

/**
 * Step Progress Indicator component for Owner Registration Wizard.
 */
@Composable
fun AuthStepProgress(
    currentStep: Int,
    modifier: Modifier = Modifier,
    totalSteps: Int = 4
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { idx ->
            val stepNum = idx + 1
            val isDone = stepNum < currentStep
            val isCurrent = stepNum == currentStep

            val color by animateColorAsState(
                targetValue = when {
                    isDone -> PresenceDesign.Positive
                    isCurrent -> PresenceDesign.Brand
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                label = "stepProgressColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
