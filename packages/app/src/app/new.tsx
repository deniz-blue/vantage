import { useMutation } from "@tanstack/react-query";
import { useRouter, useFocusEffect, useLocalSearchParams } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { OpenEvnt } from "@evnt/types";
import { EventsManager } from "@vantage/core";
import { Container } from "../components/base/Container";
import { EventForm } from "../components/event/editor/EventForm";
import { createEditor } from "../components/event/editor/editor";
import { Box } from "../components/base/Box";
import { Text } from "../components/base/Text";
import { FontSize, IconSize } from "../theme/sizing";
import { Select } from "../components/base/input/Select";
import { Divider } from "../components/base/Divider";
import { Button } from "../components/base/button/Button";
import { Colors } from "../theme/colors";
import { IconDatabase } from "@tabler/icons-react-native";
import { OpenEvntSchema } from "@evnt/schema";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";
import Animated from "react-native-reanimated";
import { useKeyboardHeight } from "../hooks/useKeyboardHeight";
import { ActionBackButton } from "../components/app/ActionBackButton";
import { AtprotoDid, CanonicalResourceUri } from "@atcute/lexicons/syntax";
import { useAtAccounts, useAtClient } from "@vantage/atproto";
import { AtUserAvatar, AtUserHandle } from "../components/user/AtUserCard";
import { now } from "@atcute/tid";

const validJson = (str: any): boolean => {
	if (typeof str !== "string") return false;
	try {
		const parsed = JSON.parse(str);
		return OpenEvntSchema.safeParse(parsed).success;
	} catch {
		return false;
	}
};

type SaveTarget = { type: "local" } | { type: "atproto"; did: AtprotoDid };

export default function NewEventPage() {
	const insets = useSafeAreaInsets();
	const keyboardHeight = useKeyboardHeight();
	const router = useRouter();
	const { data } = useLocalSearchParams();
	const [form, setForm] = useState<OpenEvnt>({ v: "0.1", name: {} });
	const [saveTarget, setSaveTarget] = useState<SaveTarget>({ type: "local" });
	const editor = useMemo(() => createEditor(form, setForm), [form, setForm]);

	useFocusEffect(
		useCallback(() => {
			if (data && typeof data === "string" && validJson(data)) {
				const parsed = JSON.parse(data);
				setForm(parsed);
			} else setForm({ v: "0.1", name: {} });
		}, [data]),
	);

	const save = useMutation({
		mutationFn: async () => {
			console.log("Saving event...");

			const data: OpenEvnt = { ...editor.value, $type: "directory.evnt.event" };
			const raw = JSON.stringify(data);

			switch (saveTarget.type) {
				case "local": {
					return await EventsManager.addEventWithCache({
						source: { type: "local" },
						format: { type: "directory.evnt.event" },
						raw,
						parsed: data,
						error: null,
					});
				}
				case "atproto": {
					const did = saveTarget.did;
					if (!useAtAccounts.getState().accounts[did])
						throw new Error("No account found for DID: " + did);
					if (useAtAccounts.getState().activeDid !== did) await useAtClient.getState().signIn(did);
					const { client } = useAtClient.getState();
					if (!client) throw new Error("No AT Protocol client available");
					const res = await client.post("com.atproto.repo.putRecord", {
						input: {
							collection: "directory.evnt.event",
							record: data as any,
							repo: did,
							rkey: now(),
						},
					});
					if (!res.ok) throw new Error(res.data.error + ": " + res.data.message);
					return await EventsManager.addEvent({
						source: { type: "at", uri: res.data.uri as CanonicalResourceUri },
						format: { type: "directory.evnt.event" },
					});
				}
			}
		},
		onSuccess: (id) => {
			router.push(`/event/${id}`);
			setForm({ v: "0.1", name: {} });
		},
	});

	return (
		<Box flex={1}>
			<Box component={Animated.ScrollView}>
				<Box component={SafeAreaView} flex={1}>
					<Container size="sm" flex={1}>
						<Box py="md" flex={1}>
							<Box gap="md" flex={1}>
								<Box direction="row" align="center" gap="sm">
									<ActionBackButton />
									<Text fz={FontSize.h1} fw="bold">
										Create Event
									</Text>
								</Box>

								<Box>
									<SaveTargetSelect value={saveTarget} onChange={setSaveTarget} />
								</Box>

								<Divider />

								<EventForm editor={editor} />
							</Box>
						</Box>
						<Box h={200} />
						<Box h={insets.bottom} />
						<Animated.View
							style={{
								height: keyboardHeight,
							}}
						/>
					</Container>
				</Box>
			</Box>
			<Box pos="absolute" style={{ bottom: insets.bottom }} w="100%">
				<Container size="sm" flex={1} pb="md">
					<Button variant="primary" w="100%" loading={save.isPending} onPress={() => save.mutate()}>
						{save.isPending ? "Saving…" : "Save"}
					</Button>
				</Container>
			</Box>
		</Box>
	);
}

export const SaveTargetSelect = ({
	value,
	onChange,
}: {
	value: SaveTarget;
	onChange: (value: SaveTarget) => void;
}) => {
	const accounts = useAtAccounts((s) => s.accounts);

	const accountTargets = useMemo(
		() =>
			Object.values(accounts).map(
				(account) =>
					({
						type: "atproto",
						did: account.did,
					}) as SaveTarget,
			),
		[accounts],
	);

	const renderItem = useCallback(({ value }: { value: SaveTarget }) => {
		if (value.type === "local")
			return (
				<Box direction="row" gap="xs" align="center">
					<IconDatabase size={IconSize.sm} color={Colors.Text} />
					<Text fz={FontSize.sm}>This Device</Text>
				</Box>
			);
		if (value.type === "atproto")
			return (
				<Box direction="row" gap="xs" align="center">
					<AtUserAvatar did={value.did} size={IconSize.sm} />
					<Text fz={FontSize.sm}>
						<AtUserHandle did={value.did} />
					</Text>
				</Box>
			);
	}, []);

	return (
		<Select<SaveTarget>
			label="Save to"
			value={value}
			onChange={onChange}
			data={[{ type: "local" }, ...accountTargets]}
			renderItem={renderItem}
			searchable={false}
		/>
	);
};
