package com.recentr.feature.onboarding.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.feature.onboarding.R

/**
 * Full-width "Continue" CTA. Until the step is answered it is drawn as the Figma *Soft* button
 * (name screen, empty state) and ignores taps; the design-system `enabled = false` state would
 * fade it to 40% instead, which the design never shows.
 */
@Composable
internal fun ContinueButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.onb_continue),
) {
    RecentrButton(
        text = text,
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (!enabled) disabled() },
        style = if (enabled) RecentrButtonStyle.Primary else RecentrButtonStyle.Soft,
    )
}
