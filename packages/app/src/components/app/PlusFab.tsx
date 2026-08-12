import { Fragment, useRef, useState } from "react";
import { Fab } from "../base/Fab";
import { IconPlus, IconRss, IconWorldDownload } from "@tabler/icons-react-native";
import { usePathname, useRouter } from "expo-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { EventsManager, ResolvedEventContext, Infer, eventQueryFn } from "@vantage/core";
import { Sheet, SheetRef } from "../base/sheet/Sheet";
import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { TextInput } from "../base/input/TextInput";
import { Text } from "../base/Text";
import { EventCard } from "../event/card/EventCard";
import { Colors } from "../../theme/colors";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { FontSize, IconSize, Radius } from "../../theme/sizing";
import { ButtonBase } from "../base/ButtonBase";

export const PlusFab = () => {
	const router = useRouter();
	const path = usePathname();
	const insets = useSafeAreaInsets();
	const sheet = useRef<SheetRef>(null);
	const [state, setState] = useState<"fab" | "import" | "jsonfeed">("fab");

	const show = path !== "/settings";

	const reset = () => {
		setState("fab");
		sheet.current?.dismiss();
	};

	if (!show) return null;

	return (
		<Fragment>
			<Fab
				aria-label="Add Event"
				wrapperProps={{ style: { marginBottom: 56 + insets.bottom } }}
				icon={<IconPlus color="#fff" />}
				onPress={() => sheet.current?.present()}
			/>

			<Sheet ref={sheet} onDidDismiss={reset}>
				<Box>
					{state === "fab" && (
						<Box gap="md" w="100%">
							<Text ta="center">Add Event</Text>
							<Box direction="row" w="100%">
								{(
									[
										{
											label: "From Link",
											type: "fn",
											icon: IconWorldDownload,
											onRun: () => setState("import"),
										},
										{
											label: "Create New",
											icon: IconPlus,
											onRun: () => {
												router.push("/new");
												reset();
											},
										},

										{
											label: "From Feed",
											type: "fn",
											icon: IconRss,
											onRun: () => {
												router.push("/feed");
												reset();
											},
										},
									] as const
								).map(({ label, icon: Icon, onRun }, i) => {
									const primary = i === 1;

									return (
										<Box key={i} flex={primary ? 2 : 1}>
											<Box p="xs">
												<ButtonBase onPress={() => onRun()}>
													<Box
														bg={Colors.BackgroundLight}
														radius={Radius.Default}
														py="md"
														gap="xs"
														align="center"
														justify="center"
													>
														<Icon size={IconSize.lg} color={Colors.Text} />
														<Text fz={FontSize.sm}>{label}</Text>
													</Box>
												</ButtonBase>
											</Box>
										</Box>
									);
								})}
							</Box>
						</Box>
					)}

					{state === "import" && <Importer onClose={reset} />}
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
			const resolved = Infer.fromString(uri);
			return await eventQueryFn(resolved);
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
				<Text>Import via Link</Text>
				<Text fz={FontSize.sm} c={Colors.TextDimmed} ta="center">
					The event will be synced to this device.
				</Text>
			</Box>

			<TextInput
				label="Link to Event"
				value={uri}
				onChangeText={setUri}
				placeholder="https://example.com/sample-event.evnt.json"
				autoCapitalize="none"
				autoCorrect={false}
				editable={!resolved.isLoading}
			/>

			{(resolved.error ?? resolved.data?.error) && (
				<Box p="sm" bg={Colors.Red + "11"} radius={8}>
					<Text fz={13} c={Colors.Red}>
						{String(resolved.error ?? resolved.data?.error?.message)}
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
