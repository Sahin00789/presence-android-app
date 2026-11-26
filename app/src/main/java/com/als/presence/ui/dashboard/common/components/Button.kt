package com.als.presence.ui.dashboard.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceAnimations
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant

private fun PresenceSize.height(): Dp = when (this) {
    PresenceSize.Sm -> PresenceDesign.SmallButtonHeight
    PresenceSize.Md -> PresenceDesign.DefaultButtonHeight
    PresenceSize.Lg -> 52.dp
}

private fun PresenceSize.fontSize() = when (this) {
    PresenceSize.Sm -> 13.sp
    PresenceSize.Md -> 14.sp
    PresenceSize.Lg -> 16.sp
}

private fun PresenceSize.iconSize() = when (this) {
    PresenceSize.Sm -> 16.dp
    PresenceSize.Md -> 18.dp
    PresenceSize.Lg -> 20.dp
}

@Composable
fun PresenceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PresenceVariant = PresenceVariant.Primary,
    size: PresenceSize = PresenceSize.Md,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.96f else 1f,
        animationSpec = PresenceAnimations.SpringBouncy,
        label = "btnSpringScale"
    )

    val (containerColor, contentColor, border) = when (variant) {
        PresenceVariant.Primary -> Triple(
            PresenceDesign.Brand,
            Color.White,
            null
        )
        PresenceVariant.Secondary -> Triple(
            MaterialTheme.colorScheme.surface,
            PresenceDesign.Ink,
            BorderStroke(PresenceDesign.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
        )
        PresenceVariant.Ghost -> Triple(
            Color.Transparent,
            PresenceDesign.Brand,
            null
        )
        PresenceVariant.Soft -> Triple(
            PresenceDesign.BrandSoft,
            PresenceDesign.BrandDark,
            null
        )
        PresenceVariant.Danger -> Triple(
            PresenceDesign.DangerSoft,
            PresenceDesign.DangerText,
            null
        )
        PresenceVariant.DangerSolid -> Triple(
            PresenceDesign.Danger,
            Color.White,
            null
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(size.height())
            .scale(scale),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(PresenceDesign.RadiusControl),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        border = border,
        elevation = null,
        contentPadding = PaddingValues(horizontal = if (size == PresenceSize.Sm) 12.dp else 16.dp),
        interactionSource = interactionSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size.iconSize()),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(size.iconSize())
                )
            }
            Text(
                text = text,
                fontSize = size.fontSize(),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
