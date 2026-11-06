# Architecture

Vantage is a calendar for events from many sources and formats. One Kotlin Multiplatform
codebase builds the Android app and the web app at vantage.tsx.lt (Wasm). Five Gradle
modules, each depending only on the one above it:

| Module | Targets | Holds |
|---|---|---|
| `:openevnt` | jvm, android, wasmJs | The Open Evnt model (`directory.evnt.OpenEvnt`), its JSON, and converters from other formats (`directory.evnt.convert`, Community Lexicon among them). No Vantage code. |
| `:core` | jvm, android, wasmJs (node) | Everything about events that isn't storage or UI: where an event comes from (`EventSource`), fetching and resolving it (`EventFetcher`, `EventResolver`, `ResolvedEvent`), links (`LinkRecognition`, `EventsLink`, `AppSchemes`), feeds (`EventFeeds`), queries and time ranges, the calendar grid, Markdown, weather, place search. |
| `:storage` | jvm, android, wasmJs (browser) | The Room database (`VantageDatabase`, `Entities`, `Daos`) and `EventStore`, which the UI talks to. Backup export and import. |
| `:app` | android, wasmJs | The Compose Multiplatform UI: `VantageApp`, `Navigator` and `Route`, the screens in `screens/`, shared widgets in `ui/`. `WebAddresses.kt` maps every route to a path. |
| `:androidApp` | android | The Android application: manifest, link filters, build types, signing, Play publishing. |

## Events

An event row (`EventEntity`) is its id, source and format. `EventSource` says where it
came from: `Local` (made in the app), `Http`, `At` (an AT Protocol record), `Folio`,
`MediaWiki`, `WebDav`, or `Calendar` (an entry of a subscribed calendar). `EventFormat`
says how to read the raw text: Open Evnt, Community Lexicon or iCalendar. The last fetch
sits in `EventCacheEntity`: the raw text, the parsed `OpenEvnt`, and any fetch or parse
error. Everything is converted to `OpenEvnt`, so the UI only ever reads one model.

Remote events are fetched again on a schedule (`RefreshSchedule`): every 7 days at most,
more often as the start nears, down to every 30 minutes, and until 30 days after the end.
`EventDifferences` says what changed between two versions.

## Feeds and calendars

A feed is something to browse, not something already saved: an Open Evnt index
(`EventFeed.OpenEvntIndex`, paged through `next`) or the records in an AT repository
(`EventFeed.AtRepository`). Home tiles show feeds, and the last listing of each is kept
(`FeedSnapshots`) so a tile has content before the network answers.

A subscribed calendar (`SubscribedCalendars`) is different: its entries go into My Events
and stay in step with the source.

How links bring events and feeds in: [links.md](links.md). Building, testing and
installing: [building.md](building.md).
