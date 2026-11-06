package blue.deniz.vantage.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.app.screens.ANDROID_EARLY_ACCESS_URL
import blue.deniz.vantage.app.ui.AppIcons
import blue.deniz.vantage.app.ui.Icon
import blue.deniz.vantage.app.ui.IconSize
import blue.deniz.vantage.app.ui.Spacing
import blue.deniz.vantage.app.ui.Text

@Composable
fun AndroidAppBanner() {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth().height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { uriHandler.openUri(ANDROID_EARLY_ACCESS_URL) }
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Smartphone, Modifier.padding(end = Spacing.sm), size = IconSize.xs, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Vantage for Android", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text("Get the App", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Icon(AppIcons.ChevronRight, Modifier.padding(start = Spacing.xs), size = 14.dp, tint = MaterialTheme.colorScheme.primary)
    }
}
