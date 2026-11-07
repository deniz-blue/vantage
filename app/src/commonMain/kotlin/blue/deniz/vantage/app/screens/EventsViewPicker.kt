package blue.deniz.vantage.app.screens

import blue.deniz.vantage.app.ui.SELECTED_TINT_ALPHA
import blue.deniz.vantage.app.ui.floatingSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import blue.deniz.vantage.app.EventsView
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.floatingBorder
import blue.deniz.vantage.app.ui.ControlHeight
import blue.deniz.vantage.app.ui.FloatingIconButton

@Composable
fun EventsViewControls(views: List<EventsView>, selected: EventsView, onSelect: (EventsView) -> Unit, onNew: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        EventsViewPicker(views, selected, onSelect)
        if (onNew != null) FloatingIconButton(AppIcons.Plus, "New", onNew, size = EventsViewControlsHeight)
    }
}

@Composable
private fun EventsViewPicker(views: List<EventsView>, selected: EventsView, onSelect: (EventsView) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.floatingSurface(CircleShape).selectableGroup(),
        shape = CircleShape,
        color = Color.Transparent,
        border = floatingBorder(),
    ) {
        Row(Modifier.padding(Spacing.xs)) {
            for (view in views) EventsViewButton(view, view == selected, onClick = { onSelect(view) })
        }
    }
}

private val EventsViewControlsHeight = ControlHeight.lg + Spacing.xs * 2

val EventsViewControlsClearance = EventsViewControlsHeight + Spacing.sm * 2

@Composable
private fun EventsViewButton(view: EventsView, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(ControlHeight.lg)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = SELECTED_TINT_ALPHA) else Color.Transparent)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = view.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(view.icon(selected), tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun EventsView.icon(selected: Boolean) = when (this) {
    EventsView.List -> if (selected) AppIcons.ListSelected else AppIcons.List
    EventsView.Calendar -> if (selected) AppIcons.CalendarSelected else AppIcons.Calendar
    EventsView.Map -> if (selected) AppIcons.MapSelected else AppIcons.Map
}
