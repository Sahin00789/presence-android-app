package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.als.presence.core.designsystem.PresenceDesign

/**
 * A labelled group of fields that answer one question.
 *
 * Presence puts a schedule's fields inside their own container rather than
 * letting them run together, so the reader can see which values belong together.
 */
@Composable
fun PresenceFieldGroup(
    title: String?,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PresenceDesign.RadiusPanel))
            .background(PresenceDesign.Panel)
            .border(1.dp, PresenceDesign.Line, RoundedCornerShape(PresenceDesign.RadiusPanel))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (title != null) {
            PresenceKicker(title)
        }
        content()
        if (hint != null) {
            Text(
                text = hint,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = PresenceDesign.LabelText
            )
        }
    }
}

/** A text input in the Presence field style. */
@Composable
fun PresenceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    supporting: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PresenceDesign.Ink
        ),
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PresenceDesign.LabelText
            )
        },
        placeholder = placeholder?.let {
            { Text(text = it, fontSize = 12.5.sp, color = PresenceDesign.MutedLight) }
        },
        supportingText = supporting?.let {
            { Text(text = it, fontSize = 10.5.sp, color = PresenceDesign.LabelText) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(PresenceDesign.RadiusControl),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PresenceDesign.Brand,
            unfocusedBorderColor = PresenceDesign.InputBorder,
            focusedLabelColor = PresenceDesign.Brand,
            unfocusedLabelColor = PresenceDesign.LabelText,
            cursorColor = PresenceDesign.Brand,
            focusedContainerColor = PresenceDesign.Surface,
            unfocusedContainerColor = PresenceDesign.Surface,
            focusedTextColor = PresenceDesign.Ink,
            unfocusedTextColor = PresenceDesign.Ink
        )
    )
}

/**
 * A modal in the Presence style: white panel, hairline header, hairline footer
 * with the actions pushed to the right.
 */
@Composable
fun PresenceDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    confirmLabel: String = "Save",
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    confirmVariant: PresenceVariant = PresenceVariant.Primary,
    dismissLabel: String = "Cancel",
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PresenceDesign.Surface)
                    .border(1.dp, PresenceDesign.Line, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 15.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (eyebrow != null) {
                            PresenceKicker(eyebrow)
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = PresenceDesign.Ink
                        )
                    }
                    PresenceIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PresenceDesign.Line)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PresenceDesign.Line)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresenceButton(
                        label = dismissLabel,
                        onClick = onDismiss,
                        variant = PresenceVariant.Ghost,
                        size = PresenceSize.Sm
                    )
                    if (onConfirm != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PresenceButton(
                            label = confirmLabel,
                            onClick = onConfirm,
                            variant = confirmVariant,
                            size = PresenceSize.Sm,
                            enabled = confirmEnabled
                        )
                    }
                }
            }
        }
    }
}
