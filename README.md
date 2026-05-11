# Vantage

A reference web application for viewing events.

This application is built using React, Mantine, Zustand, Jotai, Tanstack Query and Router, SQLocal, Drizzle ORM and more.

This is a monorepo managed with pnpm, and consists of the following packages:

- `packages/app-web`: The web application, built with React and Mantine.
- `packages/core`: The core library, which contains the main logic for fetching, parsing and caching event data, as well as some utilities and types. It holds the drizzle database instance and the query client.
- `packages/db`: The database package, which contains the Drizzle ORM schema.

## For Developers

Special pages exist:

- `/embed` - iframe embeddable event card information.
  
  You must supply one of the following query parameters:
  - `source`: HTTP URL or AT URI
  - `event-data`: JSON string of OpenEvent data

- `/form` - render an event data editing form
  
  Query parameters:
  - Either `source` or `event-data` **required**, same as `/embed`
  - `redirect-to`: URL to redirect to after form submission; query parameters will be appended with the updated event data, e.g. `?data={"...`
  - `continue-text`: Text to show on the continue button (default: "Continue")
  - `title`: Title to show above the form (default: "Edit Event Data")
  - `desc`: Description to show above the form

## Diagram

```mermaid
classDiagram
	class Events
	class EventMeta
	class EventCache
	Events : string id
	EventCache : string id
	EventMeta : string id
	EventMeta : json source
	EventMeta : json format
	EventCache : json? raw
	EventCache : json? parsed
	EventCache : json? revision
	EventCache : json? error
	EventCache : json? computed
	Events --> EventMeta : references
	Events --> EventCache : references
	class SourceDefinition
	class FormatDefinition
	SourceDefinition : string type
	SourceDefinition : resolve() {raw,error,revision}
	FormatDefinition : string type
	FormatDefinition : parse() {parsed,error}
	EventMeta --> SourceDefinition : references
	EventMeta --> FormatDefinition : references
```

```mermaid
flowchart TD
	queryEvent([useEventQuery])
	db[(Database)]

	queryEvent -->|SELECT FROM event_meta, event_cache| db --> checkCache
	checkCache{Is cache valid?}
	checkCache -->|Yes| returnEvent[Return ResolvedEvent]
	checkCache -->|No| fetchSource([Fetch data from event_meta.source])
	fetchSource -->|+raw, +revision, +error| checkRaw{Did we get raw data?}
	checkRaw -->|Yes| parseFormat([Parse raw data with event_meta.format])
	checkRaw -->|No| updateCache
	parseFormat -->|+parsed, +error| updateCache[Update cache] -->|INSERT/UPDATE| db
	updateCache --> returnEvent
```

## Development

- Go to [this link](https://eventsl.ink/?setInstanceUrl=http://127.0.0.1:5173) to set the instance URL for development 

```bash
cd apps/web
pnpm i
pnpm dev
```



