# Links the app opens

Every way an event or feed gets into Vantage from outside is a link. Android, the web build
and pasted text all go through the same parsing, so a link that works in one works in all.

## Hosts and schemes

| Form | Where it is handled |
|---|---|
| `https://vantage.tsx.lt/…` | the app's own web addresses (`app/…/WebAddresses.kt`) |
| `https://eventsl.ink/…` | the Events Link forms below (`core/…/EventsLink.kt`) |
| `vantage://`, `evnt://`, `web+evnt://` | rewritten to `https://vantage.tsx.lt/` by `AppSchemes.webAddressOf`, host first: `vantage://event/abc/edit` → `/event/abc/edit` |

Android claims all of them in `androidApp/src/main/AndroidManifest.xml`. `vantage.tsx.lt` and
`eventsl.ink` are `autoVerify` filters, so they open the app only when the host's assetlinks
file lists the signing key of the installed build. Otherwise the link opens the web app,
which saves to the browser, not the phone. The custom schemes always open the app.

Text shared through Android's Share menu, and text pasted into the new event editor, is
searched with `findEventLink`. The first word that is a recognised link becomes an "Event
Link" card with Import This Event.

## Events Link forms

Paths on `eventsl.ink`, and the same paths on `vantage.tsx.lt`:

| Path | Parameters | Opens |
|---|---|---|
| `/e` or `/event` (or `?type=event`) | one of `at`, `url`, `data` | the event, with Import This Event |
| `/edit` (or `?type=edit`) | one of `at`, `url`, `data`; `return` (http/https, required); `state` | the editor. Save or Cancel goes back to `return` with `#event=<json>` or `#cancelled=true`, plus `state` |
| `/index` (or `?type=index`) | `url` (http/https) | an Open Evnt index as a feed |

- `at`: an `at://` URI of an Open Evnt or community.lexicon.calendar.event record.
- `url`: anything `recognizeLink` reads (next section).
- `data`: a whole Open Evnt document as URL-encoded JSON. No hosting needed, so this is
  the way to hand someone a one-off event. The document needs `v`, `id` and `name`; an
  `urn:uuid:` id is fine. Added, it becomes a local event, and a link carrying the same
  `id` opens that stored copy.

```
https://vantage.tsx.lt/event?data=%7B%22v%22%3A%220.2%22%2C%22id%22%3A%22urn%3Auuid%3A…%22%2C%22name%22%3A%7B%22en%22%3A%22…%22%7D%2C%22occurrences%22%3A%5B%7B%22start%22%3A%222026-11-10T16%3A00%5BEurope%2FVilnius%5D%22%7D%5D%7D
```

## What a `url` can point at

`recognizeLink` in `core/…/LinkRecognition.kt`, first match wins:

| Link | Source | Format |
|---|---|---|
| `at://…` with a known collection | `EventSource.At` | Open Evnt or Community Lexicon |
| an `eventsl.ink` link | whatever its `at`, `url` or `data` gives | |
| `<folio host>/events/<id>[?token=…]` | `EventSource.Folio` (the token allows editing) | Open Evnt |
| `https://<site>/rest.php/v1/page/<title>` | `EventSource.MediaWiki` | Open Evnt |
| any other http(s) URL | `EventSource.Http` | `.json` Open Evnt, `.ics` iCalendar, else guessed |

## App addresses

From `WebAddresses.kt`. Useful for deep links and for checking a screen in the web build.

| Address | Screen |
|---|---|
| `/` | Home |
| `/events` (also `/list`, `/calendar`) | My Events |
| `/event/<id>`, `/event/<id>/edit` | an event, its editor |
| `/new?text=…&day=YYYY-MM-DD` | the new event editor, prefilled |
| `/feed?type=index&url=…` | an Open Evnt index |
| `/feed?type=at&repo=…&collection=…` | the records in an AT repository |
| `/settings`, `/settings/<page>` | settings; pages: appearance, notifications, backup, home, tags, calendars, following, about, developer-tools |
| `/setup` | first-run setup |
