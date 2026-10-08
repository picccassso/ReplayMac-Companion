package com.replaymac.companion.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.replaymac.companion.ui.theme.AppColors
import kotlinx.coroutines.launch

private data class TutorialPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val highlights: List<Pair<String, String>>
)

private val tutorialPages = listOf(
    TutorialPage(
        title = "Your Pocket Stream Deck",
        subtitle = "Control ReplayMac on your Mac directly from your Android phone without alt-tabbing or interrupting gameplay.",
        icon = Icons.Default.Videocam,
        iconColor = AppColors.Accent,
        highlights = listOf(
            "1-Tap Saving" to "Instantly save your best moments to disk while gaming.",
            "Live Deck Monitoring" to "Real-time buffer timeline, memory meter, and session status."
        )
    ),
    TutorialPage(
        title = "Wi-Fi & USB Tethering",
        subtitle = "Connect seamlessly to your Mac on the local network or over USB with ultra-low latency.",
        icon = Icons.Default.Cable,
        iconColor = AppColors.AccentVariant,
        highlights = listOf(
            "Local Wi-Fi" to "Automatically discovers ReplayMac running on your network via Bonjour/NSD.",
            "Zero-Latency USB" to "Plug in via USB and run 'adb reverse tcp:7375 tcp:7375' for instant response."
        )
    ),
    TutorialPage(
        title = "Replay Buffer vs Session",
        subtitle = "Two distinct ways to capture gameplay and screen highlights:",
        icon = Icons.Default.History,
        iconColor = AppColors.Warning,
        highlights = listOf(
            "Replay Buffer (RAM)" to "Constantly holds the last 30s or 5m in RAM so you can save highlights retroactively after they happen.",
            "Session Recording" to "Standard continuous video recording until you manually stop it."
        )
    ),
    TutorialPage(
        title = "Desk Deck Mode & Audio",
        subtitle = "Place your phone on a desk dock for effortless capture and quick audio toggles.",
        icon = Icons.Default.Lightbulb,
        iconColor = AppColors.Success,
        highlights = listOf(
            "Deck On / Off" to "Keep the phone screen awake indefinitely during extended gaming sessions.",
            "Instant Audio Muting" to "Toggle microphone and system audio capture with a single tap.",
            "Recent Session Clips" to "Review recent clips and open them immediately in Finder on Mac."
        )
    )
)

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == tutorialPages.size - 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 0.92f)
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, AppColors.GlassBorder, RoundedCornerShape(22.dp)),
            color = AppColors.BackgroundSecondary,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isLandscape) 16.dp else 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button & header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Guide (${pagerState.currentPage + 1}/${tutorialPages.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AppColors.TextTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .background(AppColors.SurfaceGlassSubtle, CircleShape)
                            .border(1.dp, AppColors.GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { pageIndex ->
                    val page = tutorialPages[pageIndex]
                    TutorialPageContent(
                        page = page,
                        isLandscape = isLandscape
                    )
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 18.dp))

                // Bottom Controls: Indicator dots + Next / Done button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(tutorialPages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 18.dp else 6.dp,
                                label = "dotWidth"
                            )
                            val color by animateColorAsState(
                                targetValue = if (isSelected) AppColors.Accent else AppColors.GlassBorder,
                                label = "dotColor"
                            )

                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(width)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isLastPage) {
                            TextButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = AppColors.TextSecondary
                                )
                            ) {
                                Text(
                                    text = "Skip",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isLastPage) "Get Started" else "Next",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialPageContent(
    page: TutorialPage,
    isLandscape: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Icon
        Box(
            modifier = Modifier
                .size(if (isLandscape) 46.dp else 56.dp)
                .clip(CircleShape)
                .background(page.iconColor.copy(alpha = 0.15f))
                .border(1.dp, page.iconColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.iconColor,
                modifier = Modifier.size(if (isLandscape) 24.dp else 30.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                fontSize = if (isLandscape) 18.sp else 20.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppColors.TextSecondary,
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 16.dp))

        // Highlight cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            page.highlights.forEach { (label, detail) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SurfaceGlassSubtle)
                        .border(0.5.dp, AppColors.GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(page.iconColor, CircleShape)
                    )
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                        )
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AppColors.TextSecondary,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
