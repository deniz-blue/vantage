package blue.deniz.vantage.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import blue.deniz.vantage.app.screens.PickerHeader
import blue.deniz.vantage.ui.AppIcons
import blue.deniz.vantage.ui.MenuItem
import blue.deniz.vantage.ui.Spacing

class CopyOption(val label: String, val supportingText: String? = null, val text: () -> String)

@Composable
fun CopyOptionsRow(options: List<CopyOption>, onOpenPage: () -> Unit) {
    val only = options.singleOrNull()
    if (only != null) return CopyMenuItem("Copy ${only.label}", only.supportingText, only.text)
    if (options.isNotEmpty()) MenuItem("Copy", onClick = onOpenPage, leadingIcon = AppIcons.Copy, trailingIcon = AppIcons.ChevronRight)
}

@Composable
fun CopyOptionsPage(options: List<CopyOption>, onBack: () -> Unit) {
    PickerHeader("Copy", onBack, Modifier.padding(horizontal = Spacing.md))
    Column {
        for (option in options) CopyMenuItem(option.label, option.supportingText, option.text)
    }
}
