package blue.deniz.vantage.app

import androidx.compose.runtime.saveable.Saver
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt

val OpenEvntSaver: Saver<OpenEvnt, String> = Saver(
    save = { OpenEvnt.json.encodeToString(OpenEvnt.serializer(), it) },
    restore = { OpenEvnt.json.decodeFromString(OpenEvnt.serializer(), it) },
)

val OccurrenceSaver: Saver<Occurrence?, String> = Saver(
    save = { it?.let { occurrence -> OpenEvnt.json.encodeToString(Occurrence.serializer(), occurrence) } },
    restore = { OpenEvnt.json.decodeFromString(Occurrence.serializer(), it) },
)
