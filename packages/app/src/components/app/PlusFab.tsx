import { Fragment, useState } from "react";
import { Fab } from "../base/Fab";
import { IconPlus } from "@tabler/icons-react-native";
import { usePathname, useRouter } from "expo-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
	inferSourceFormat,
	eventQueryFnNoId,
	EventsManager,
	ResolvedEventContext,
} from "@vantage/core";
import { Sheet } from "../base/sheet/Sheet";
import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { TextInput } from "../base/input/TextInput";
import { Text } from "../base/Text";
import { EventCard } from "../event/card/EventCard";
import { Colors } from "../../theme/colors";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { ActionButtonList } from "../actions/ActionButton";

export const PlusFab = () => {
	const router = useRouter();
	const path = usePathname();
	const insets = useSafeAreaInsets();
	const [state, setState] = useState<"none" | "fab" | "import">("none");

	const show = path === "/" || path === "/list";

	if (!show) return null;

	return (
		<Fragment>
			<Fab
				aria-label="Add Event"
				wrapperProps={{ style: { marginBottom: 56 + insets.bottom } }}
				icon={<IconPlus color="#fff" />}
				onPress={() => setState("fab")}
			/>

			<Sheet open={state !== "none"} onClose={() => setState("none")}>
				{state === "fab" && (
					<ActionButtonList
						actions={[
							{
								label: "Create Event",
								type: "fn",
								onRun: () => {
									setState("none");
									router.push("/new");
								},
							},
							{
								label: "Import Event",
								type: "fn",
								onRun: () => setState("import"),
							},
						]}
					/>
				)}

				{state === "import" && <Importer onClose={() => setState("none")} />}
			</Sheet>
		</Fragment>
	);
};

export const Importer = ({ onClose }: { onClose?: () => void }) => {
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
		<Box gap="md">
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
				<Button variant="primary" w="100%" loading={save.isPending} onPress={() => save.mutate()}>
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
