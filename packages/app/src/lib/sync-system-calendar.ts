import { createCalendar, getCalendars } from "expo-calendar";

const CALENDAR_TITLE = "Vantage";
const CALENDAR_ID = "vantage";

export const upsertCalendar = async () => {
	const calendars = await getCalendars();
	const vantageCalendar = calendars.find((c) => c.title === CALENDAR_TITLE || c.id === CALENDAR_ID);
	if (!vantageCalendar) {
		const newCalendar = await createCalendar({
			allowsModifications: false,
			title: CALENDAR_TITLE,
			id: CALENDAR_ID,
		});
		return newCalendar;
	}
	return vantageCalendar;
};

export const syncCalendar = async () => {};
