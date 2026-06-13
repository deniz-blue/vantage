import { useResolvedEvent } from "@vantage/core";
import { Card } from "../../base/Card";
import { Text } from "../../base/Text";
import { Colors } from "../../../theme/colors";

const getErrorMeta = (err: Vantage.Error) => {
	const color = err.kind === "json-parse" || err.kind === "validation" ? "yellow" as const : "red" as const;
	const message = (
		err.kind === "fetch" ? "Fetch Error" :
		err.kind === "json-parse" ? "JSON Parse Error" :
		err.kind === "validation" ? "Validation Error" :
		err.kind === "xrpc" ? "XRPC Error" :
		err.kind ?? "Error"
	);
	return { color, message };
};

const bgTint = (color: string) => color === "yellow" ? Colors.Yellow + "18" : Colors.Red + "11";
const borderTint = (color: string) => color === "yellow" ? Colors.Yellow + "44" : Colors.Red + "33";
const textColor = (color: string) => color === "yellow" ? Colors.Yellow : Colors.Red;

export const EventDetailsError = () => {
	const { error } = useResolvedEvent();

	if (!error) return null;

	const meta = getErrorMeta(error);

	return (
		<Card
			p="sm"
			bg={bgTint(meta.color)}
			style={{ borderWidth: 1, borderColor: borderTint(meta.color) }}
		>
			<Text c={textColor(meta.color)} fz={13}>
				{meta.message}: {error.message}
			</Text>
		</Card>
	);
};
