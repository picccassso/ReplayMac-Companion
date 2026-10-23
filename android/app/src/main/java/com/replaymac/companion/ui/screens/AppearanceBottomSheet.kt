package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.demo.DemoAtmosphere
import com.replaymac.companion.data.demo.DemoAtmospheres
import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.ui.components.GlassCard
import com.replaymac.companion.ui.components.RowSeparator
import com.replaymac.companion.ui.components.SectionHeader
import com.replaymac.companion.ui.components.glass
import com.replaymac.companion.ui.components.glassPressable
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii
import com.replaymac.companion.ui.theme.AppTheme
import com.replaymac.companion.ui.theme.SurfaceStyle
import com.replaymac.companion.ui.theme.ThemeHex
import com.replaymac.companion.ui.theme.ThemePalette

/**
 * How the app looks: whether it follows the Mac's theme, and what that theme
 * is. In demo mode it picks the demo Mac's atmosphere instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceBottomSheet(
    /** The theme in the Mac's latest snapshot; null from a ReplayMac that doesn't share one. */
    macTheme: CompanionTheme?,
    matchMac: Boolean,
    isDemo: Boolean,
    onMatchMacChange: (Boolean) -> Unit,
    onSelectDemoAtmosphere: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = if (isLandscape) 760.dp else 640.dp,
        containerColor = AppTheme.palette.card,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )

            if (isDemo) {
                DemoAtmospherePicker(
                    selectedId = macTheme?.atmosphere ?: DemoAtmospheres.DEFAULT_ID,
                    onSelect = onSelectDemoAtmosphere
                )
            } else {
                MatchMacRow(matchMac = matchMac, onChange = onMatchMacChange)
                MacThemeSection(theme = macTheme, isApplied = matchMac)
            }
        }
    }
}

@Composable
private fun MatchMacRow(matchMac: Boolean, onChange: (Boolean) -> Unit) {
    val palette = AppTheme.palette
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadii.Medium.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = matchMac, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Match my Mac",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Use the colours from ReplayCap on your Mac.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }
            // The row handles the toggle, so the switch itself only shows the state.
            Switch(
                checked = matchMac,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.onAccent,
                    checkedTrackColor = palette.accent,
                    checkedBorderColor = palette.accent,
                    uncheckedThumbColor = AppColors.TextSecondary,
                    uncheckedTrackColor = AppColors.SurfaceGlass,
                    uncheckedBorderColor = AppColors.GlassBorder
                )
            )
        }
    }
}

/**
 * What the Mac is wearing, shown dimmed while the phone isn't following it,
 * or a nudge to update ReplayCap when the Mac shares no colours.
 */
@Composable
private fun MacThemeSection(theme: CompanionTheme?, isApplied: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Your Mac's Theme")
        val dark = theme?.dark
        if (theme == null || dark == null) {
            AppearanceInfoBox("Update ReplayCap on your Mac to share its theme.")
            return@Column
        }
        val emphasis by animateFloatAsState(if (isApplied) 1f else 0.55f, label = "macThemeEmphasis")
        GlassCard(
            modifier = Modifier.fillMaxWidth().alpha(emphasis),
            shape = RoundedCornerShape(AppRadii.Medium.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = theme.atmosphereName.ifBlank { "Custom" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = SurfaceStyle.fromWire(theme.surfaceStyle).displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
                // The Mac's own colours, before the phone's fallbacks or the native rule.
                SwatchRow(listOf(dark.canvas, dark.card, dark.accent, dark.accentSecondary))
            }
        }
    }
}

@Composable
private fun DemoAtmospherePicker(selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Atmospheres")
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadii.Medium.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DemoAtmospheres.all.forEachIndexed { index, atmosphere ->
                    AtmosphereRow(
                        atmosphere = atmosphere,
                        isSelected = atmosphere.id == selectedId,
                        onSelect = { onSelect(atmosphere.id) }
                    )
                    if (index < DemoAtmospheres.all.lastIndex) RowSeparator(startIndent = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun AtmosphereRow(atmosphere: DemoAtmosphere, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(onClick = onSelect, shape = RoundedCornerShape(0.dp), role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { if (isSelected) contentDescription = "${atmosphere.name}, selected" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = atmosphere.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        SwatchRow(listOf(atmosphere.primary, atmosphere.secondary))
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = AppTheme.palette.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** Small colour dots with a hairline rim, so dark colours still read against the sheet. A bad hex shows the default card. */
@Composable
private fun SwatchRow(hexes: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        hexes.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(ThemeHex.parse(hex) ?: ThemePalette.DefaultCard, CircleShape)
                    .border(0.75.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }
    }
}

@Composable
private fun AppearanceInfoBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(AppRadii.Medium.dp), AppColors.SurfaceGlassSubtle, rimAlpha = 0.6f)
            .padding(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}
