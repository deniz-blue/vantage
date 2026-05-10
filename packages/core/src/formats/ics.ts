import { defineEventFormat } from "../lib/format";
import { convertFromVEvent } from "@evnt/convert/icalendar";
import ICAL from "ical.js";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"ics": {};
		}
	}
}

defineEventFormat({
	type: "ics",
	parse: (raw) => {
		const jCalData = ICAL.parse(raw);
		const vEvent = new ICAL.Component(jCalData);
		const converted = convertFromVEvent(vEvent);
		return {
			parsed: converted,
		};
	}
});
