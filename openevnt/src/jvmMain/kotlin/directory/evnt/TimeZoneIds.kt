package directory.evnt

import kotlinx.datetime.TimeZone

internal actual val ianaTimeZoneIds: Set<String> by lazy { TimeZone.availableZoneIds }
