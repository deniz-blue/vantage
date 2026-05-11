import { Alert, type BoxProps } from "@mantine/core";
import { getEnvelopeErrorMeta } from "./envelope-error-meta";
import { useResolvedEvent } from "@vantage/core";

export const EnvelopeErrorAlert = (props: BoxProps) => {
	const { error } = useResolvedEvent();
	if (!error) return null;
	const { color, message, details, status } = getEnvelopeErrorMeta(error);

	return (
		<Alert
			color={color}
			variant="light"
			title={message + (status ? ` (${status})` : "")}
			styles={{
				body: {
					whiteSpace: "pre",
				},
			}}
			{...props}
		>
			{details}
		</Alert>
	);
};
