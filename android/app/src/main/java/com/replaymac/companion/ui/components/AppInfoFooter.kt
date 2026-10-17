package com.replaymac.companion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replaymac.companion.AppLinks
import com.replaymac.companion.ui.theme.AppColors

/**
 * States that the app needs ReplayMac on a Mac, and links to it and to the
 * privacy policy. Shown under the connection options and in the quick guide,
 * so both are reachable whether or not a Mac is connected.
 */
@Composable
fun AppInfoFooter(
    modifier: Modifier = Modifier,
    showsRequirement: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (showsRequirement) {
            Text(
                text = "Works with ReplayMac for Mac, free on GitHub or as ReplayCap on the Mac App Store.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            FooterLink(text = "Get ReplayMac for Mac", url = AppLinks.REPLAYMAC_FOR_MAC)
            Text(
                text = "·",
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
                modifier = Modifier.clearAndSetSemantics {}
            )
            FooterLink(text = "Privacy Policy", url = AppLinks.PRIVACY_POLICY)
        }
    }
}

/** Small accent text that opens [url] in the browser, padded out to a comfortable touch target. */
@Composable
private fun FooterLink(text: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = AppColors.Accent,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = "Open in browser") { uriHandler.openUri(url) }
            .padding(horizontal = 6.dp, vertical = 14.dp)
    )
}
