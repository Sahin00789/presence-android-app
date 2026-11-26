package com.als.presence.ui.auth.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.core.designsystem.PresenceDesign

/**
 * Primary Horizontal Presence Logo Component.
 * Features Symbol on Left + Wordmark on Right.
 */
@Composable
fun PresenceHorizontalLogo(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_symbol_color),
            contentDescription = stringResource(id = R.string.presence_logo_description),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.presence_brand_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            color = if (isDarkTheme) Color.White else colorResource(id = R.color.presence_navy)
        )
    }
}

/**
 * Vertical Presence Logo Component (Auth / Registration Screens).
 * Features Symbol Centered above Wordmark.
 */
@Composable
fun PresenceVerticalLogo(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colorResource(id = R.color.presence_navy))
                .border(
                    width = 1.5.dp,
                    color = colorResource(id = R.color.presence_border_dark),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo_symbol_color),
                contentDescription = stringResource(id = R.string.presence_logo_description),
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(id = R.string.presence_brand_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = if (isDarkTheme) Color.White else colorResource(id = R.color.presence_navy)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(id = R.string.presence_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkTheme) Color(0xFF94A3B8) else colorResource(id = R.color.presence_text_secondary),
            fontSize = 12.5.sp
        )
    }
}

/**
 * App Icon Component.
 */
@Composable
fun PresenceAppIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colorResource(id = R.color.presence_navy))
            .border(
                width = 1.5.dp,
                color = colorResource(id = R.color.presence_border_dark),
                shape = RoundedCornerShape(26.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_symbol_color),
            contentDescription = stringResource(id = R.string.presence_logo_description),
            modifier = Modifier.size(68.dp)
        )
    }
}

/**
 * Full Brand Identity Showcase Preview (Light Theme).
 */
@Preview(showBackground = true, backgroundColor = 0xFFF4F8FF, name = "Presence Logo Suite (Light)")
@Composable
fun PresenceLogoLightSuitePreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorResource(id = R.color.presence_light_bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    text = "Presence Brand Identity Suite (Light)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.presence_navy)
                )

                // A. Horizontal Logo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "A. Primary Horizontal Logo",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(id = R.color.presence_text_secondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PresenceHorizontalLogo(isDarkTheme = false)
                }

                // B. Vertical Logo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "B. Vertical Logo",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(id = R.color.presence_text_secondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PresenceVerticalLogo(isDarkTheme = false)
                }

                // C. App Icon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "C. App Launcher Icon",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(id = R.color.presence_text_secondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PresenceAppIcon()
                }

                // D. Monochrome Version
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "D. Monochrome Logo Symbol",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(id = R.color.presence_text_secondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_symbol_monochrome),
                        contentDescription = "Monochrome Symbol",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full Brand Identity Showcase Preview (Dark Theme).
 */
@Preview(showBackground = true, backgroundColor = 0xFF030C1E, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Presence Logo Suite (Dark)")
@Composable
fun PresenceLogoDarkSuitePreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF030C1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    text = "Presence Brand Identity Suite (Dark)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // A. Horizontal Logo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "A. Primary Horizontal Logo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PresenceHorizontalLogo(isDarkTheme = true)
                }

                // B. Vertical Logo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "B. Vertical Logo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PresenceVerticalLogo(isDarkTheme = true)
                }

                // C. App Icon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "C. App Launcher Icon",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PresenceAppIcon()
                }

                // D. Monochrome Version
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "D. Monochrome Logo Symbol",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_symbol_monochrome),
                        contentDescription = "Monochrome Symbol",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}
