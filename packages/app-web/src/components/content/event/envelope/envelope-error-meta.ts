import z from "zod";

export const getEnvelopeErrorMeta = (err: Vantage.Error) => {
	let color: "red" | "yellow" = "red";
	let message = err.kind ?? "Error";
	let details = err.message ?? "";

	if (err.kind === "json-parse" || err.kind === "validation") {
		color = "yellow";
	} else {
		color = "red";
	};

	if (err.kind === "fetch") details = err.message;
	if (err.kind === "json-parse") details = err.message;
	if (err.kind === "validation") details = z.prettifyError({
		issues: err.issues ?? [],
	});

	if (err.kind === "fetch") message = "Fetch Error";
	if (err.kind === "json-parse") message = "JSON Parse Error";
	if (err.kind === "validation") message = "Validation Error";
	if (err.kind === "xrpc") message = "XRPC Error";

	return {
		color,
		message,
		details,
		status: err.status,
	} as const;
};
