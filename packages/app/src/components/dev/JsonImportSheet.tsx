import { useState, useCallback } from "react";
import { ScrollView, TouchableOpacity } from "react-native";
import { OpenEvntSchema } from "@evnt/schema";
import { EventsManager } from "@vantage/core";
import { Sheet } from "../base/Sheet";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { TextInput } from "../base/TextInput";
import { Colors } from "../../theme/colors";

export const JsonImportSheet = ({
	open,
	onClose,
}: {
	open: boolean;
	onClose: () => void;
}) => {
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
		onClose();
	}, [onClose]);

	return (
		<Sheet open={open} onClose={handleClose} height={0.8}>
			<ScrollView
				style={{ flex: 1 }}
				keyboardShouldPersistTaps="handled"
				contentContainerStyle={{ padding: 16, gap: 12 }}
			>
				<Text fz={18} fw="bold">Import Event JSON</Text>
				<Text fz={13} c="TextDimmed">
					Paste an OpenEvnt event as JSON below. It will be validated and saved as a local event.
				</Text>

				<TextInput
					label="JSON"
					multiline
					numberOfLines={12}
					style={{ minHeight: 200, fontFamily: "monospace", fontSize: 12 }}
					placeholder='{\n  "v": "0.1",\n  "name": { "en": "My Event" },\n  ...\n}'
					value={raw}
					onChangeText={setRaw}
					autoCapitalize="none"
					autoCorrect={false}
				/>

				{status && (
					<Box
						p="sm"
						bg={status.kind === "success" ? Colors.Green + "18" : Colors.Red + "11"}
						style={{ borderRadius: 8 }}
					>
						<Text
							fz={13}
							c={status.kind === "success" ? Colors.Green : Colors.Red}
						>
							{status.message}
						</Text>
					</Box>
				)}

				<Box direction="row" gap={8} justify="flex-end">
					<TouchableOpacity onPress={handleClose}>
						<Box
							bg="BackgroundLight"
							px="md"
							py="sm"
							radius={8}
						>
							<Text fz={14}>Cancel</Text>
						</Box>
					</TouchableOpacity>

					<TouchableOpacity
						onPress={handleImport}
						disabled={importing || !raw.trim()}
					>
						<Box
							bg={importing || !raw.trim() ? "BackgroundLight" : "Primary"}
							px="md"
							py="sm"
							radius={8}
						>
							<Text
								fz={14}
								c={importing || !raw.trim() ? "TextDimmed" : "Text"}
							>
								{importing ? "Importing…" : "Import"}
							</Text>
						</Box>
					</TouchableOpacity>
				</Box>
			</ScrollView>
		</Sheet>
	);
};
