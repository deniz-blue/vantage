export const VantageError = new class {
	getColor(error: Vantage.Error): string {
		switch (error.kind) {
			case "fetch":
				return "red";
			case "json-parse":
				return "yellow";
			case "validation":
				return "yellow";
			case "xrpc":
				return "red";
			default:
				return "red";
		}
	}

	getMessage(error: Vantage.Error): string {
		switch (error.kind) {
			case "fetch":
				return "Fetch Error";
			case "json-parse":
				return "JSON Parse Error";
			case "validation":
				return "Validation Error";
			case "xrpc":
				return "XRPC Error";
			default:
				return error.kind ?? "Error";
		}
	}
};
