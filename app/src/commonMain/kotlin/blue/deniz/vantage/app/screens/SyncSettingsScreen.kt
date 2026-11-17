package blue.deniz.vantage.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.SmallsyncException
import blue.deniz.vantage.storage.StoreSync
import blue.deniz.vantage.storage.SyncStatus
import blue.deniz.vantage.storage.syncProblemOf
import blue.deniz.vantage.ui.AppIcons
import blue.deniz.vantage.ui.Button
import blue.deniz.vantage.ui.ListRow
import blue.deniz.vantage.ui.ListSection
import blue.deniz.vantage.ui.MenuItem
import blue.deniz.vantage.ui.Spacing
import blue.deniz.vantage.ui.TextInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SyncStatusReading(val status: SyncStatus?)

fun StoreSync.observeStatusReadings(): Flow<SyncStatusReading> = observeStatus().map(::SyncStatusReading)

fun storeLabel(storeUrl: String) = storeUrl.substringAfter("://").trimEnd('/')

fun SyncStatus.summary(): String =
    if (problem != null) {
        listOfNotNull(problem, waitingChangesLabel(waitingChanges)).joinToString(" · ")
    } else {
        listOf(if (lastSyncedAt == null) "Not Synced Yet" else "Up to Date", eventCountLabel(syncedEvents), tagCountLabel(syncedTags)).joinToString(" · ")
    }

private fun waitingChangesLabel(count: Int) = when (count) {
    0 -> null
    1 -> "1 Change Waiting"
    else -> "$count Changes Waiting"
}

@Composable
fun SyncSettingsScreen(state: AppState) {
    val sync = state.storeSync ?: return
    val reading = collectAcrossSession("store sync status", sync) { sync.observeStatusReadings() }
    SettingsPage("Sync", onBack = { state.navigator.goBack() }) {
        when (val status = reading?.status) {
            null -> if (reading != null) ConnectForm(sync)
            else -> ConnectedRows(sync, status)
        }
    }
}

@Composable
private fun ConnectForm(sync: StoreSync) {
    val scope = rememberCoroutineScope()
    var storeUrl by rememberSaveable { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    var storeProblem by remember { mutableStateOf<String?>(null) }
    var tokenProblem by remember { mutableStateOf<String?>(null) }
    val canConnect = storeUrl.startsWith("https://") && token.isNotBlank() && !connecting

    fun connect() {
        if (!canConnect) return
        connecting = true
        storeProblem = null
        tokenProblem = null
        scope.launch {
            try {
                sync.connect(storeUrl, token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failed: Exception) {
                if (failed is SmallsyncException && failed.isTokenRefused) tokenProblem = syncProblemOf(failed) else storeProblem = syncProblemOf(failed)
            } finally {
                connecting = false
            }
        }
    }

    Column(Modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TextInput(
            value = storeUrl,
            onValueChange = {
                storeUrl = it.trim()
                storeProblem = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = "Store",
            placeholder = "https://…",
            problem = storeProblem,
            showsProblem = storeProblem != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        TextInput(
            value = token,
            onValueChange = {
                token = it
                tokenProblem = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = "Token",
            problem = tokenProblem,
            showsProblem = tokenProblem != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { connect() }),
        )
        Button("Connect", onClick = ::connect, primary = true, enabled = canConnect, busy = connecting, modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs))
    }
}

@Composable
private fun ConnectedRows(sync: StoreSync, status: SyncStatus) {
    val scope = rememberCoroutineScope()
    var changingToken by rememberSaveable { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var savingToken by remember { mutableStateOf(false) }
    var tokenProblem by remember { mutableStateOf<String?>(null) }

    fun saveToken() {
        if (token.isBlank() || savingToken) return
        savingToken = true
        tokenProblem = null
        scope.launch {
            try {
                sync.changeToken(token)
                changingToken = false
                token = ""
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failed: Exception) {
                tokenProblem = syncProblemOf(failed)
            } finally {
                savingToken = false
            }
        }
    }

    ListSection {
        ListRow(
            storeLabel(status.storeUrl),
            icon = AppIcons.Cloud,
            supporting = status.summary(),
            color = if (status.problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (status.isTokenRefused) {
            MenuItem("Change Token", onClick = { changingToken = !changingToken }, leadingIcon = AppIcons.Key)
        } else {
            MenuItem("Sync Now", onClick = { scope.launch { sync.sync() } }, leadingIcon = AppIcons.Refresh, busy = status.isSyncing)
        }
    }
    if (changingToken && status.isTokenRefused) {
        Column(Modifier.padding(horizontal = Spacing.md).padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TextInput(
                value = token,
                onValueChange = {
                    token = it
                    tokenProblem = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = "Token",
                problem = tokenProblem,
                showsProblem = tokenProblem != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { saveToken() }),
            )
            Button("Save", onClick = ::saveToken, primary = true, enabled = token.isNotBlank(), busy = savingToken, modifier = Modifier.fillMaxWidth())
        }
    }
    ListSection(Modifier.padding(top = Spacing.md)) {
        MenuItem("Disconnect", onClick = { scope.launch { sync.disconnect() } }, leadingIcon = AppIcons.Disconnect, danger = true)
    }
}
