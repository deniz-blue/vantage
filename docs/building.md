# Building, testing, installing

JDK 21. Gradle daemons on the development machine are shared with other projects, so
never run `./gradlew --stop`.

## Tests

```
./gradlew allTests            # every module
./gradlew :core:allTests      # one module
```

`:core` and `:openevnt` tests run on the JVM and wasm. `:storage` tests run on the JVM with
bundled SQLite.

## Android

Three build types in `androidApp/build.gradle.kts`:

| Type | Package | Label | Signed with |
|---|---|---|---|
| `debug` | `lt.tsx.vantage` | Vantage | the signing properties below, else the debug key |
| `release` | `lt.tsx.vantage` | Vantage | the same; minified and shrunk |
| `dev` | `lt.tsx.vantage.dev` | Vantage Dev | always the debug key; otherwise the release build |

`dev` installs next to the Play build, so both can be on one phone with separate data.

```
./gradlew :androidApp:assembleDev -PandroidAbis=arm64-v8a
adb install -r androidApp/build/outputs/apk/dev/androidApp-dev.apk
```

`-PandroidAbis` limits the native libraries to the listed ABIs (comma-separated), which
makes the APK smaller and the build faster.

Signing for `debug` and `release` comes from `-PsigningProperties=<file>` (keys
`storePassword`, `keyAlias`, optional `keyPassword`) and `-PsigningKeystore=<file>`.

## Google Play

Gradle Play Publisher, track `alpha` (closed testing). Credentials come from
`-PplayCredentials=<service account JSON>` or `ANDROID_PUBLISHER_CREDENTIALS`. The listing
and release notes live in `androidApp/src/main/play/`.

```
./gradlew :androidApp:bootstrapListing        # downloads the listing, changes nothing
./gradlew :androidApp:publishReleaseBundle    # builds, signs and uploads to alpha
```

The `versionCode` has to beat the highest one Play has seen.

## Web

```
./gradlew :app:wasmJsBrowserDevelopmentExecutableDistribution
./gradlew :app:wasmJsBrowserDistribution
```

The production bundle lands in `app/build/dist/wasmJs/productionExecutable`. vantage.tsx.lt
is the Cloudflare Pages project `vantage-web`. A deploy uploads that folder without its
`.map` files.

## Reading and writing a phone's events over adb

The app exports `EventsProvider` (`app/src/androidMain/…/EventsProvider.kt`) at
`content://<package>.v1.events/`, with `events`, `markers` and `periods` under it. Callers
need `<package>.permission.READ_EVENTS` or `WRITE_EVENTS`, except `adb shell` (uid 2000),
which may always read and write.

```
adb shell content query  --user 0 --uri 'content://lt.tsx.vantage.dev.v1.events/events?search=FOSDEM' --projection id:json
adb shell content insert --user 0 --uri content://lt.tsx.vantage.dev.v1.events/events --bind 'json:s:{"v"\:"0.2",…}'
adb shell content insert --user 0 --uri content://lt.tsx.vantage.dev.v1.events/events --bind 'url:s:https\://…'
adb shell content delete --user 0 --uri content://lt.tsx.vantage.dev.v1.events/events/<id>
```

- `json` adds an Open Evnt document as a local event. `url` adds a link the way the app's
  Add button does, and returns the stored id if that source is already saved.
- `content` rejects a value with bare colons ("Binding not well formed"), so escape every
  `:` in the value as `\:`.
- On phones with a work profile or Secure Folder, `content` without `--user 0` may try
  another user and fail with a SecurityException.
- Query parameters on `events`: `after` and `before` (ISO instants), `search` (name text).
- `update` replaces an event at `events/<id>`, and only when its source can be edited in
  the app.
