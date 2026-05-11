import { Badge, Stack, Text, Tooltip, type BoxProps } from "@mantine/core";
import { getEnvelopeErrorMeta } from "./envelope-error-meta";
import { useResolvedEvent } from "@vantage/core";

export const EnvelopeErrorBadge = (props: BoxProps) => {
	const { error } = useResolvedEvent();
	if (!error) return null;

	const { color, message, details, status } = getEnvelopeErrorMeta(error);

	return (
		<Tooltip label={(
			<Stack align="center" gap={4}>
				<Text fw="bold" span inherit>{message}</Text>
				<Text inherit span style={{ whiteSpace: "pre" }}>
					{details}
				</Text>
			</Stack>
		)} multiline>
			<Badge color={color} variant="outline" {...props}>
				{status ?? "ERR"}
			</Badge>
		</Tooltip>
	);
};
