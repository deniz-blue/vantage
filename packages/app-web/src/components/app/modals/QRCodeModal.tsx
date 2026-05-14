import { Button, Paper, Stack } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { QRCode } from "../../../lib/util/qrcode";
import { useEffect } from "react";

export const QRCodeModal = ({
	innerProps: { value },
	context,
	id,
}: ContextModalProps<{
	value: string;
}>) => {
	useEffect(() => {
		context.updateModal({
			modalId: id,
			size: "sm",
		});
	}, []);

	return (
		<Stack>
			<QRCode
				value={value}
			/>
			<Button
				color="gray"
				onClick={() => context.closeModal(id)}
			>
				Close
			</Button>
		</Stack>
	);
}
