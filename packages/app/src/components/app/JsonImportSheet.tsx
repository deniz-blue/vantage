import { useState, useCallback } from "react";
import { OpenEvntSchema } from "@evnt/schema";
import { EventsManager } from "@vantage/core";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { TextInput } from "../base/input/TextInput";
import { Colors } from "../../theme/colors";
import { FontSize } from "../../theme/sizing";
import { Button } from "../base/button/Button";

export const JsonImportSheetContent = ({ onClose }: { onClose?: () => void }) => {
	const [raw, setRaw] = useState("");
	const [status, setStatus] = useState<{ kind: "success" | "error"; message: string } | null>(null);
	const [importing, setImporting] = useState(false);

	const handleImport = useCallback(async () => {
		setStatus(null);

		if (!raw.trim()) {
			setStatus({ kind: "error", message: "JSON input is empty." });
			return;
		}

		setImporting(true);

		try {
			const json = JSON.parse(raw);
			const result = OpenEvntSchema.safeParse(json);

			if (!result.success) {
				const issues = result.error.issues
					.slice(0, 5)
					.map((i) => `  • ${i.path.join(".")}: ${i.message}`)
					.join("\n");
				setStatus({
					kind: "error",
					message: `Validation failed:\n${issues}`,
				});
				return;
			}

			const parsed = result.data;

			await EventsManager.addEventWithCache({
				source: { type: "local" },
				format: { type: "directory.evnt.event" },
				raw,
				parsed,
				error: null,
			});

			setStatus({ kind: "success", message: "Event saved!" });
			setRaw("");
		} catch (err) {
			setStatus({
				kind: "error",
				message: `JSON parse error: ${err instanceof Error ? err.message : String(err)}`,
			});
		} finally {
			setImporting(false);
		}
	}, [raw]);

	const handleClose = useCallback(() => {
		setRaw("");
		setStatus(null);
		onClose?.();
	}, []);

	return (
		<Box gap="md">
			<Text fz={FontSize.h1} fw="bold">
				Import Event JSON
			</Text>
			<Text c="TextDimmed">
				Paste an OpenEvnt event as JSON below. It will be validated and saved as a local event.
			</Text>

			<TextInput
				label="JSON"
				multiline
				numberOfLines={12}
				style={{ minHeight: 200, fontFamily: "monospace", fontSize: 12 }}
				placeholder={JSON.stringify({ v: "0.1", name: { en: "My Event" } }, null, 2)}
				value={raw}
				onChangeText={setRaw}
				verticalAlign="top"
				autoCapitalize="none"
				autoCorrect={false}
			/>

			{status && (
				<Box
					p="sm"
					bg={status.kind === "success" ? Colors.Green + "18" : Colors.Red + "11"}
					style={{ borderRadius: 8 }}
				>
					<Text fz={FontSize.sm} c={status.kind === "success" ? Colors.Green : Colors.Red}>
						{status.message}
					</Text>
				</Box>
			)}

			<Box direction="row" gap={8} justify="flex-end">
				<Button onPress={handleClose}>Cancel</Button>
				<Button variant="primary" onPress={handleImport} loading={importing} disabled={!raw.trim()}>
					{importing ? "Importing…" : "Import"}
				</Button>
			</Box>
		</Box>
	);
};
