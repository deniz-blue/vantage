import { Button, JsonInput, Stack } from "@mantine/core";
import { modals, type ContextModalProps } from "@mantine/modals";
import { useState } from "react";
import { prettifyError, type z } from "zod";
import { EventDataSchema, type EventData } from "@evnt/schema";
import { AsyncAction } from "../../data/AsyncAction";
import { dbShortcuts } from "../../../db/db-shortcuts";

export const ImportJSONModal = ({
	context,
	id: modalId,
}: ContextModalProps<{}>) => {
	const [json, setJson] = useState("");

	let error = "";
	let parsed = null;
	let result: z.ZodSafeParseResult<EventData> | null = null;
	try {
		parsed = JSON.parse(json);
		result = EventDataSchema.safeParse(parsed);
		if (!result.success) {
			error = prettifyError(result.error);
		}
	} catch (e) {
		error = "" + e;
	}

	return (
		<Stack>
			<JsonInput
				value={json}
				onChange={setJson}
				minRows={5}
				autosize
				placeholder="Paste event JSON here..."
				error={error}
				styles={{
					error: { whiteSpace: "pre-wrap" },
				}}
			/>
			<AsyncAction
				action={async () => {
					if (!result || !result.success) return;
					await dbShortcuts.insertLocalEvent(JSON.stringify(result.data), { type: "directory.evnt.event" });
					modals.close(modalId);
				}}
			>
				{({ loading, onClick }) => (
					<Button
						loading={loading}
						disabled={!!error || !json || !result || !result.success}
						onClick={onClick}
						color="green"
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
