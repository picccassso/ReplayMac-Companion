package com.replaymac.companion.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * A dialog whose window blurs the screen behind it on Android 12+, the
 * closest Android gets to a real glass sheet. Devices without cross-window
 * blur (or with it disabled for battery) fall back to a plain dim.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val blurRadius = with(LocalDensity.current) { 28.dp.roundToPx() }
        SideEffect {
            window ?: return@SideEffect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = blurRadius }
                window.setDimAmount(0.35f)
            } else {
                window.setDimAmount(0.65f)
            }
        }
        content()
    }
}
