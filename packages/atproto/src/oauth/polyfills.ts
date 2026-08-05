if (!URL.canParse) {
	URL.canParse = (url: string | URL, base?: string) => {
		try {
			new URL(url as string, base);
			return true;
		} catch {
			return false;
		}
	};
}

// Hermes/React Native AbortSignal lacks throwIfAborted
if (typeof AbortSignal !== "undefined" && !AbortSignal.prototype.throwIfAborted) {
	AbortSignal.prototype.throwIfAborted = function () {
		if (this.aborted) {
			throw this.reason ?? new DOMException("The signal has been aborted", "AbortError");
		}
	};
}
