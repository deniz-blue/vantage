import { parseCanonicalResourceUri } from "@atcute/lexicons";
import { useAtAccounts } from "@vantage/atproto";

export const useCanEditEvent = (resolved: Vantage.ResolvedEvent | null) => {
	const accounts = useAtAccounts((s) => s.accounts);

	if (!resolved) return false;
	if (!resolved.id) return false;

	return !!(
		resolved.source.type === "local" ||
		(resolved.source.type === "at" &&
			accounts[parseCanonicalResourceUri(resolved.source.uri).repo]) ||
		resolved.source.type === "mediawiki" ||
		(resolved.source.type === "folio" && resolved.source.editToken)
	);
};
