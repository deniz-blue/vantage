package directory.evnt

import kotlinx.datetime.TimeZone

// kotlinx-datetime on wasm knows only UTC until this package is imported, which registers the IANA rules with js-joda.
@JsModule("@js-joda/timezone")
private external object JsJodaTimeZoneModule

internal actual val ianaTimeZoneIds: Set<String> by lazy { JsJodaTimeZoneModule.run { TimeZone.availableZoneIds } }
