package blue.deniz.vantage.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun FailureRow(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    details: List<String> = emptyList(),
    retrying: Boolean = false,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = Spacing.sm, end = if (onRetry == null) Spacing.sm else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(AppIcons.AlertTriangle, tint = MaterialTheme.colorScheme.error)
        Column(Modifier.weight(1f).padding(vertical = Spacing.sm)) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            for (detail in details) {
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (onRetry != null) {
            IconButton(AppIcons.Refresh, if (retrying) "Retrying" else "Retry", onClick = onRetry, enabled = !retrying, busy = retrying)
        }
    }
}
