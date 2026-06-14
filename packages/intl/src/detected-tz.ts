// Intl.DateTimeFormat is available on Hermes (bundles ICU4C).

export const getDetectedTz = (): string => {
	try {
		return Intl.DateTimeFormat().resolvedOptions().timeZone;
	} catch {
		return "UTC";
	}
};
