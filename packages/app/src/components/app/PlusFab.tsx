import { Fragment, useState } from "react";
import { Fab } from "../base/Fab";
import { Spacing } from "../../theme/spacing";
import { IconPlus } from "@tabler/icons-react-native";
import { usePathname, useRouter } from "expo-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { inferSourceFormat, eventQueryFnNoId, EventsManager, ResolvedEventContext } from "@vantage/core";
import { Sheet } from "../base/Sheet";
import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { TextInput } from "../base/input/TextInput";
import { Text } from "../base/Text";
import { EventCard } from "../event/card/EventCard";
import { Colors } from "../../theme/colors";

export const PlusFab = () => {
	const router = useRouter();
	const path = usePathname();
	const [open, setOpen] = useState(false);

	const show = path === "/" || path === "/list";

	if (!show) return null;

	return (
		<Fragment>
			<Fab
				wrapperProps={{ style: { bottom: 56 + Spacing.md } }}
				icon={<IconPlus color="#fff" />}
				actions={[
					{
						label: "Create",
						onPress: () => router.push("/new"),
					},
					{
						label: "Import",
						onPress: () => setOpen(true),
					}
				]}
			/>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Importer onClose={() => setOpen(false)} />
			</Sheet>
		</Fragment>
	);
};

export const Importer = ({
	onClose,
}: {
	onClose?: () => void;
}) => {
	const [uri, setUri] = useState("");

	const resolved = useQuery({
		queryKey: ["import-resolve", uri],
		queryFn: async () => {
			const { source, format } = await inferSourceFormat(uri);
			return await eventQueryFnNoId(source, format);
		},
		enabled: uri.trim().length > 0,
		retry: false,
	});

	const save = useMutation({
		mutationFn: async () => {
			const ev = resolved.data;
			if (!ev) return;
			await EventsManager.addEventWithCache({
				source: ev.source,
				format: ev.format,
				raw: JSON.stringify(ev.data),
				parsed: ev.data,
				error: ev.error,
			});
		},
		onSuccess: onClose,
	});

	return (
		<Box p="md" gap="md">
			<TextInput
				label="URL"
				value={uri}
				onChangeText={setUri}
				placeholder="https://example.com/file.json"
				autoCapitalize="none"
				autoCorrect={false}
				editable={!resolved.isLoading}
			/>

			{resolved.error && (
				<Box p="sm" bg={Colors.Red + "11"} radius={8}>
					<Text fz={13} c={Colors.Red}>
						{String(resolved.error)}
					</Text>
				</Box>
			)}

			{resolved.data && (
				<ResolvedEventContext value={resolved.data}>
					<EventCard />
				</ResolvedEventContext>
			)}

			{resolved.data ? (
				<Button
					variant="primary"
					w="100%"
					loading={save.isPending}
					onPress={() => save.mutate()}
				>
					{save.isPending ? "Saving…" : "Save to This Device"}
				</Button>
			) : (
				<Button
					w="100%"
					disabled={!uri.trim() || resolved.isLoading}
					loading={resolved.isLoading}
					onPress={() => resolved.refetch()}
				>
					{resolved.isLoading ? "Resolving…" : "Import"}
				</Button>
			)}
		</Box>
	);
};
