import { Fragment, useState } from "react";
import { Fab } from "../base/Fab";
import { IconPlus, IconRss, IconWorldDownload } from "@tabler/icons-react-native";
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
import { FontSize, IconSize } from "../../theme/sizing";
import { Loader } from "../base/Loader";

export const PlusFab = () => {
	const router = useRouter();
	const path = usePathname();
	const insets = useSafeAreaInsets();
	const [state, setState] = useState<"none" | "fab" | "import" | "jsonfeed">("none");

	const show = path !== "/settings";

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
				<Box p="md">
					{state === "fab" && (
						<ActionButtonList
							actions={[
								{
									label: "Create Event",
									type: "fn",
									icon: <IconPlus size={IconSize.sm} color={Colors.Text} />,
									onRun: () => {
										setState("none");
										router.push("/new");
									},
								},
								{
									label: "Import Event from URL",
									type: "fn",
									icon: <IconWorldDownload size={IconSize.sm} color={Colors.Text} />,
									onRun: () => setState("import"),
								},
								{
									label: "Import Events from JSON Feed",
									type: "fn",
									icon: <IconRss size={IconSize.sm} color={Colors.Text} />,
									onRun: () => setState("jsonfeed"),
								},
							]}
						/>
					)}

					{state === "import" && <Importer onClose={() => setState("none")} />}
					{state === "jsonfeed" && <JsonFeedImporter onClose={() => setState("none")} />}
				</Box>
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
			<Box align="center">
				<Text>Import from the internet</Text>
				<Text fz={FontSize.sm} c={Colors.TextDimmed} ta="center">
					Import an event via its URL. The event will be synced to this device.
				</Text>
			</Box>

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

export const JsonFeedImporter = ({ onClose: _ }: { onClose?: () => void }) => {
	const [feedUrl, setFeedUrl] = useState("");

	const query = useQuery({
		queryKey: ["jsonfeed", feedUrl],
		queryFn: async () => {
			const feed = (await fetch(feedUrl).then((res) => res.json())) as {
				items: {
					id: string;
					url?: string;
					title?: string;
					content_text?: string;
				}[];
			};
			return feed;
		},
		enabled: feedUrl.trim().length > 0,
		retry: false,
	});

	return (
		<Box gap="md" justify="center">
			<Box align="center">
				<Text>Import from JSON Feed</Text>
				<Text fz={FontSize.sm} c={Colors.TextDimmed}>
					Pick events to import from a JSON Feed
				</Text>
			</Box>

			<TextInput
				label="Feed URL"
				value={feedUrl}
				onChangeText={setFeedUrl}
				placeholder="https://example.com/feed.json"
				autoCapitalize="none"
				autoCorrect={false}
				editable={!query.isLoading}
				error={query.error ? String(query.error) : undefined}
				rightSection={query.isLoading ? <Loader /> : undefined}
			/>

			{query.data && (
				<Box gap="sm">
					{query.data.items.map((item) => (
						<Box key={item.id} p="sm" bg={Colors.Dark1} radius={8}>
							<Text fz={FontSize.sm} fw="bold">
								{item.title || item.url || item.id}
							</Text>
							{item.content_text && (
								<Text fz={FontSize.sm} c={Colors.TextDimmed}>
									{item.content_text}
								</Text>
							)}
						</Box>
					))}
				</Box>
			)}
		</Box>
	);
};
