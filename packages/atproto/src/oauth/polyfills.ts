// URL.canParse is missing in some React Native environments
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
