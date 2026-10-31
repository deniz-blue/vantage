package blue.deniz.vantage.core

import directory.evnt.InvalidOpenEvntException
import directory.evnt.convert.ConvertOptions
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlin.time.Clock

private fun Throwable.describe() = message ?: this::class.simpleName ?: "Unknown error"

private fun Throwable.toParseError() = when (this) {
    is EventException -> error
    is InvalidOpenEvntException -> EventError(EventError.Kind.Validation, "The event does not follow the OpenEvnt schema", issues = issues)
    is SerializationException -> EventError(EventError.Kind.JsonParse, describe())
    is IllegalArgumentException -> EventError(EventError.Kind.Parse, describe())
    else -> EventError(EventError.Kind.Unknown, describe())
}

class EventResolver(
    private val fetcher: EventFetcher,
    private val convertOptions: ConvertOptions = ConvertOptions(),
    private val clock: Clock = Clock.System,
) {
    suspend fun resolve(event: ResolvedEvent): ResolvedEvent = parseIfMissing(if (event.raw == null) fetch(event) else event)

    suspend fun refresh(event: ResolvedEvent): ResolvedEvent = parseIfMissing(fetch(event))

    suspend fun fetch(event: ResolvedEvent): ResolvedEvent {
        val source = event.source as? EventSource.Remote ?: return event
        val fetched = try {
            fetcher.fetch(source)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failed: EventException) {
            return event.copy(fetchError = failed.error)
        } catch (failed: Exception) {
            return event.copy(fetchError = EventError(EventError.Kind.Fetch, failed.describe()))
        }
        return event.copy(
            raw = fetched.raw,
            data = null,
            fetchError = null,
            revision = fetched.revision,
            format = if (event.format == EventFormat.Unknown) inferEventFormat(fetched.raw) else event.format,
            updatedAt = clock.now(),
        )
    }

    fun parse(event: ResolvedEvent): ResolvedEvent {
        val raw = event.raw ?: return event
        val format = if (event.format == EventFormat.Unknown) inferEventFormat(raw) else event.format
        return try {
            event.copy(format = format, data = parseEvent(raw, format, event.source, convertOptions), parseError = null)
        } catch (failed: Exception) {
            event.copy(format = format, data = null, parseError = failed.toParseError())
        }
    }

    private fun parseIfMissing(event: ResolvedEvent) = if (event.data == null) parse(event) else event
}
