import { Button, Stack, TextInput } from "@mantine/core";
import { modals, type ContextModalProps } from "@mantine/modals";
import { useState } from "react";
import { AsyncAction } from "../../data/AsyncAction";
import { dbShortcuts } from "../../../db/db-shortcuts";
import { inferSourceFormat } from "@vantage/core";

export const ImportURLModal = ({
	context,
	id: modalId,
}: ContextModalProps<{}>) => {
	const [url, setUrl] = useState("");
	const [error, setError] = useState<string | null>(null);

	return (
		<Stack>
			<TextInput
				value={url}
				onChange={e => setUrl(e.currentTarget.value)}
				placeholder="URL to event JSON..."
				error={error}
				styles={{
					error: { whiteSpace: "pre-wrap" },
				}}
			/>
			<AsyncAction
				action={async () => {
					try {
						await dbShortcuts.insertEventMeta(await inferSourceFormat(url));
						context.closeModal(modalId);
					} catch (error) {
						setError("" + error);
					}
				}}
			>
				{({ loading, onClick }) => (
					<Button
						disabled={!url}
						loading={loading}
						onClick={onClick}
					>
						Import
					</Button>
				)}
			</AsyncAction>
			<Button
				color="gray"
				onClick={() => modals.close(modalId)}
			>
				Cancel
			</Button>
		</Stack>
	);
}
