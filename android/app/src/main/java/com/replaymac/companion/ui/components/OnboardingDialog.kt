package com.replaymac.companion.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import kotlinx.coroutines.launch

private data class Highlight(val icon: ImageVector, val title: String, val detail: String)

private data class TutorialPage(val title: String, val subtitle: String, val highlights: List<Highlight>)

private val tutorialPages = listOf(
    TutorialPage(
        title = "ReplayMac, from your phone",
        subtitle = "A remote for ReplayMac on your Mac. Save clips and check on recording without leaving your game.",
        highlights = listOf(
            Highlight(Icons.Rounded.Download, "Save replays", "Tap once to save the last 30 seconds, or longer."),
            Highlight(Icons.Rounded.Speed, "See what’s happening", "Buffer progress, memory use and session time.")
        )
    ),
    TutorialPage(
        title = "Connecting",
        subtitle = "Turn on Companion Server in ReplayMac’s settings first.",
        highlights = listOf(
            Highlight(Icons.Rounded.Wifi, "Wi-Fi", "Your Mac shows up automatically when you’re on the same network."),
            Highlight(Icons.Rounded.Usb, "USB", "Plug in, then run ‘adb reverse tcp:7375 tcp:7375’ on your Mac.")
        )
    ),
    TutorialPage(
        title = "Replay buffer and sessions",
        subtitle = "Two ways to capture your screen.",
        highlights = listOf(
            Highlight(Icons.Rounded.History, "Replay buffer", "Keeps the last few seconds or minutes, so you can save after something happens."),
            Highlight(Icons.Rounded.FiberManualRecord, "Session recording", "Records everything until you stop it.")
        )
    ),
    TutorialPage(
        title = "A few more things",
        subtitle = "Handy when your phone is propped up next to you.",
        highlights = listOf(
            Highlight(Icons.Rounded.Lightbulb, "Deck mode", "Keeps the screen on. Tap the bulb at the top of Home."),
            Highlight(Icons.AutoMirrored.Rounded.VolumeUp, "Audio", "Mute the microphone or system audio."),
            Highlight(Icons.Rounded.Movie, "This session", "Clips you save show up on Home, ready to reveal in Finder.")
        )
    )
)

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == tutorialPages.lastIndex
    val shape = RoundedCornerShape(24.dp)

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth(if (isLandscape) 0.8f else 0.92f)
                .widthIn(max = 520.dp)
                .glass(shape, AppColors.SurfaceElevated)
                .padding(if (isLandscape) 18.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${pagerState.currentPage + 1} of ${tutorialPages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextTertiary,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close guide",
                    onClick = onDismiss,
                    tint = AppColors.TextSecondary,
                    size = 34.dp
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalAlignment = Alignment.Top
            ) { index ->
                TutorialPageContent(page = tutorialPages[index], isLandscape = isLandscape)
            }

            Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))
            AppInfoFooter(showsRequirement = false)
            Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PageDots(count = tutorialPages.size, current = pagerState.currentPage, modifier = Modifier.weight(1f))
                if (!isLastPage) {
                    GlassButton(
                        text = "Skip",
                        onClick = onDismiss,
                        contentColor = AppColors.TextSecondary,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                AccentButton(
                    text = if (isLastPage) "Done" else "Next",
                    onClick = {
                        if (isLastPage) onDismiss() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 20.dp else 7.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) AppColors.TextPrimary else AppColors.TextTertiary,
                label = "dotColor"
            )
            Box(Modifier.height(7.dp).width(width).background(color, CircleShape))
        }
    }
}

@Composable
private fun TutorialPageContent(page: TutorialPage, isLandscape: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
    ) {
        Spacer(Modifier.height(if (isLandscape) 4.dp else 12.dp))
        Text(text = page.title, style = MaterialTheme.typography.headlineMedium, color = AppColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(text = page.subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Spacer(Modifier.height(if (isLandscape) 12.dp else 20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 18.dp)) {
            page.highlights.forEach { highlight ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        highlight.icon,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.padding(top = 1.dp).size(20.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(highlight.title, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        Text(highlight.detail, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
            }
        }
    }
}
