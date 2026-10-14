package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.ui.components.AccentButton
import com.replaymac.companion.ui.components.GlassButton
import com.replaymac.companion.ui.components.clipDetailLine
import com.replaymac.companion.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipDetailsBottomSheet(
    clip: CompanionClipSummary,
    thumbnailBase64: String?,
    onRevealInFinder: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onDismiss: () -> Unit,
    onTrimClip: (String) -> Unit = {},
    onCopyClip: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var copiedToMac by remember(clip.id) { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = if (isLandscape) 840.dp else 640.dp,
        containerColor = AppColors.BackgroundSecondary,
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = if (isLandscape) 8.dp else 10.dp, bottom = if (isLandscape) 8.dp else 14.dp)
                    .size(width = 40.dp, height = if (isLandscape) 4.dp else 5.dp)
                    .background(AppColors.TextTertiary, CircleShape)
            )
        }
    ) {
        if (isLandscape) {
            // Side-by-side layout: thumbnail on the left, details and controls on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClipThumbnail(
                    bitmap = bitmap,
                    clip = clip,
                    modifier = Modifier.weight(1.1f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ClipInfo(clip = clip, isCompact = true)

                    ClipActions(
                        clip = clip,
                        copiedToMac = copiedToMac,
                        onRevealInFinder = onRevealInFinder,
                        onTrimClip = onTrimClip,
                        onCopyClip = { clipId ->
                            onCopyClip(clipId)
                            copiedToMac = true
                        },
                        onDeleteRequested = { showDeleteConfirmation = true },
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Portrait stacked layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ClipThumbnail(
                    bitmap = bitmap,
                    clip = clip,
                    modifier = Modifier.fillMaxWidth()
                )

                ClipInfo(clip = clip, isCompact = false)

                ClipActions(
                    clip = clip,
                    copiedToMac = copiedToMac,
                    onRevealInFinder = onRevealInFinder,
                    onTrimClip = onTrimClip,
                    onCopyClip = { clipId ->
                        onCopyClip(clipId)
                        copiedToMac = true
                    },
                    onDeleteRequested = { showDeleteConfirmation = true },
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this clip?") },
            text = { Text("\u201c${clip.filename}\u201d will be moved to the Trash on your Mac.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClip(clip.id)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = AppColors.Danger, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = AppColors.Accent, style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = AppColors.BackgroundSecondary,
            shape = RoundedCornerShape(28.dp),
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary
        )
    }
}

@Composable
private fun ClipThumbnail(
    bitmap: ImageBitmap?,
    clip: CompanionClipSummary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = clip.filename,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun ClipInfo(
    clip: CompanionClipSummary,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = clip.filename.substringBeforeLast("."),
            style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = clipDetailLine(clip),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun ClipActions(
    clip: CompanionClipSummary,
    copiedToMac: Boolean,
    onRevealInFinder: (String) -> Unit,
    onTrimClip: (String) -> Unit,
    onCopyClip: (String) -> Unit,
    onDeleteRequested: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccentButton(
                text = "Show in Finder",
                icon = Icons.Rounded.Folder,
                onClick = {
                    onRevealInFinder(clip.id)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Trim on Mac",
                icon = Icons.Rounded.ContentCut,
                onClick = {
                    onTrimClip(clip.id)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                text = if (copiedToMac) "Copied to Mac" else "Copy to Mac",
                icon = if (copiedToMac) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentColor = if (copiedToMac) AppColors.Success else AppColors.TextPrimary,
                tint = if (copiedToMac) AppColors.SuccessContainer else AppColors.SurfaceGlass,
                onClick = { onCopyClip(clip.id) },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Delete",
                icon = Icons.Rounded.Delete,
                contentColor = AppColors.Danger,
                tint = AppColors.Danger.copy(alpha = 0.12f),
                onClick = onDeleteRequested,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
