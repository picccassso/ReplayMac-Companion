package com.replaymac.companion.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        mb >= 1000 -> String.format(Locale.US, "%.1f GB", mb / 1024.0)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> "${bytes / 1024} KB"
    }
}

fun formatSavedTime(epochSeconds: Double): String {
    if (epochSeconds <= 0) return ""
    val date = Date((epochSeconds * 1000).toLong())
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}

fun clipKindTitle(kind: String): String {
    return when (kind.lowercase().trim()) {
        "replay" -> "Replay"
        "session" -> "Session"
        "extended", "extended replay" -> "Extended Replay"
        else -> kind.replaceFirstChar { it.uppercase() }
    }
}

/** "Replay · 0:30 · 24.1 MB · Saved 14:02", the desktop's recent-clip summary. */
fun clipDetailLine(clip: CompanionClipSummary): String = buildList {
    add(clipKindTitle(clip.kind))
    clip.duration?.takeIf { it > 0 }?.let { add(formatDurationSeconds(it)) }
    formatFileSize(clip.fileSize).takeIf { it.isNotEmpty() }?.let { add(it) }
    if (clip.savedAt > 0) add("Saved ${formatSavedTime(clip.savedAt)}")
}.joinToString(" · ")

@Composable
fun RecentClipItem(
    clip: CompanionClipSummary,
    thumbnailBase64: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap: ImageBitmap? = remember(thumbnailBase64) {
        if (!thumbnailBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(thumbnailBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bmp?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail box (96x54 dp, 16:9 ratio)
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = clip.filename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = clip.filename.substringBeforeLast("."),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = clipDetailLine(clip),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
