import { Button, Collapse, Stack, TextInput } from "@mantine/core";
import { modals, type ContextModalProps } from "@mantine/modals";
import { useState } from "react";
import { AsyncAction } from "../../data/AsyncAction";
import { dbShortcuts } from "../../../db/db-shortcuts";
import { useQuery } from "@tanstack/react-query";
import { webdav } from "@vantage/core";

export const ImportWebDAVModal = ({
	context,
	id: modalId,
}: ContextModalProps<{}>) => {
	const [url, setUrl] = useState("");
	const [path, setPath] = useState("/");
	const [error, setError] = useState<string | null>(null);

	const entries = useQuery({
		queryKey: ["webdav-entries", url, path],
		queryFn: async () => {
			if (!url || !path) return [];
			return await webdav.list(url, path);
		},
		enabled: !!url && !!path,
	});

	return (
		<Stack>
			<TextInput
				value={url}
				onChange={e => setUrl(e.currentTarget.value)}
				placeholder="URL to WebDAV server..."
				error={error}
			/>
			<TextInput
				value={path}
				onChange={e => setPath(e.currentTarget.value)}
				placeholder="Path to event..."
			/>


			<Collapse expanded={!!entries.data}>
				{entries.data?.map(entry => (
					<div key={entry.name}>
						{entry.name} {entry.isDirectory ? "(directory)" : ""}
					</div>
				))}
			</Collapse>


			<AsyncAction
				action={async () => {
					try {
						await dbShortcuts.insertEventMeta({
							format: { type: "directory.evnt.event" },
							source: {
								type: "webdav",
								url,
								path,
							},
						});
						context.closeModal(modalId);
					} catch (error) {
						setError("" + error);
					}
				}}
			>
				{({ loading, onClick }) => (
					<Button
						disabled={!url || !path}
						loading={loading}
						onClick={onClick}
					>
						Import
					</Button>
				)}
			</AsyncAction>
		</Stack>
	);
}
