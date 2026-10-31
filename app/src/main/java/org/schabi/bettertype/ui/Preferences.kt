package org.schabi.bettertype.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Building blocks styled after the Android 16 Settings app: titled groups of rounded, slightly separated items.

private val OUTER_CORNER = 24.dp
private val INNER_CORNER = 4.dp

@Composable
fun PreferenceCategory(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp),
    )
}

/** Stacks [items] as one group; only the group's outer corners are fully rounded. */
@Composable
fun PreferenceGroup(items: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { i, item ->
            val top = if (i == 0) OUTER_CORNER else INNER_CORNER
            val bottom = if (i == items.lastIndex) OUTER_CORNER else INNER_CORNER
            Box(
                Modifier
                    .clip(RoundedCornerShape(top, top, bottom, bottom))
                    .background(MaterialTheme.colorScheme.surfaceBright),
            ) { item() }
        }
    }
}

@Composable
fun Preference(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 72.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchPreference(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, summary: String? = null) {
    Preference(
        title,
        summary,
        Modifier.toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = { Switch(checked, onCheckedChange = null) },
    )
}

/** Explanatory text below a group, like the footer preferences in Settings. */
@Composable
fun PreferenceFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
    )
}
