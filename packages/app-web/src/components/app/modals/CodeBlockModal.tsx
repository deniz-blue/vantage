import { Button, Code, CopyButton, Stack } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { trynull } from "../../../lib/util/trynull";

export const CodeBlockModal = ({
	innerProps: { raw },
}: ContextModalProps<{
	raw: string;
}>) => {
	const asJson = trynull(() => JSON.parse(raw));
	const pretty = asJson ? JSON.stringify(asJson, null, 2) : raw;

	return (
		<Stack>
			<CopyButton
				value={pretty}
			>
				{({ copied, copy }) => (
					<Button onClick={copy} color={copied ? "teal" : undefined}>
						{copied ? "Copied" : "Copy to clipboard"}
					</Button>
				)}
			</CopyButton>

			<Code block>
				{pretty}
			</Code>
		</Stack>
	);
}
