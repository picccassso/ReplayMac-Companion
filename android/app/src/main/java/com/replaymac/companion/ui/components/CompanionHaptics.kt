package com.replaymac.companion.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

enum class SaveHapticOutcome {
    SUCCESS,
    FAILURE
}

object CompanionHaptics {
    /**
     * Pure transition classifier for save-status updates from the Mac.
     * Fires only when transitioning out of an active save ("saving" / "busy")
     * into "saved" or "failed", so initial snapshots or repeated states don't buzz.
     */
    fun saveTransitionOutcome(previousStatus: String, newStatus: String): SaveHapticOutcome? {
        val wasInProgress = previousStatus.equals("saving", ignoreCase = true) ||
            previousStatus.equals("busy", ignoreCase = true)
        if (!wasInProgress) return null

        return when {
            newStatus.equals("saved", ignoreCase = true) -> SaveHapticOutcome.SUCCESS
            newStatus.equals("failed", ignoreCase = true) -> SaveHapticOutcome.FAILURE
            else -> null
        }
    }

    fun performTap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performOutcome(view: View, outcome: SaveHapticOutcome) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (outcome) {
                SaveHapticOutcome.SUCCESS -> HapticFeedbackConstants.CONFIRM
                SaveHapticOutcome.FAILURE -> HapticFeedbackConstants.REJECT
            }
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}
