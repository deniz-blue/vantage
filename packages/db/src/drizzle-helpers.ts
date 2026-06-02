import { customType, text } from "drizzle-orm/sqlite-core";

export const uuid = (name: string) => text(name, { length: 36 });

export const timestamp = customType<{
	data: Temporal.Instant;
	driverData: number;
}>({
	dataType: () => "integer",
	fromDriver: (value) => Temporal.Instant.fromEpochMilliseconds(value),
	toDriver: (value) => value.epochMilliseconds,
});

// 2.45+ supports JSON mode for blob
export const jsonb = customType<{
	data: any;
	driverData: string;
}>({
	dataType: () => "blob",
	fromDriver: (value) => JSON.parse(value),
	toDriver: (value) => JSON.stringify(value),
});

export const bool = customType<{
	data: boolean;
	driverData: number;
}>({
	dataType: () => "integer",
	fromDriver: (value) => Boolean(value),
	toDriver: (value) => (value ? 1 : 0),
});
