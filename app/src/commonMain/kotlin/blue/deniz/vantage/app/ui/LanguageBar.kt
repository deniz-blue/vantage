package blue.deniz.vantage.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties

@Composable
fun RowScope.LanguageChips(languages: List<String>, editing: String, onEdit: (String) -> Unit, onAdd: () -> Unit) {
    for (code in languages) {
        FilterChip(
            selected = code == editing,
            onClick = { onEdit(code) },
            label = { Text(code.uppercase(), maxLines = 1) },
        )
    }
    AssistChip(
        onClick = onAdd,
        label = { Text("Language", maxLines = 1) },
        leadingIcon = { Icon(AppIcons.Plus, size = IconSize.xs) },
    )
}

@Composable
fun LanguageBar(languages: List<String>, editing: String, onEdit: (String) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .glassBar(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md)
            .focusProperties { canFocus = false },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageChips(languages, editing, onEdit, onAdd)
    }
}
