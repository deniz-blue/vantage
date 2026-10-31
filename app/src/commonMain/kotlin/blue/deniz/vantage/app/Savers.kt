package blue.deniz.vantage.app

import androidx.compose.runtime.saveable.Saver
import directory.evnt.OpenEvnt

val OpenEvntSaver: Saver<OpenEvnt, String> = Saver(
    save = { OpenEvnt.json.encodeToString(OpenEvnt.serializer(), it) },
    restore = { OpenEvnt.json.decodeFromString(OpenEvnt.serializer(), it) },
)
