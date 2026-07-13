import { useMutation } from "@tanstack/react-query";
import { useRouter, useFocusEffect, useLocalSearchParams } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { OpenEvnt } from "@evnt/types";
import { EventsManager } from "@vantage/core";
import { Container } from "../../components/base/Container";
import { EventForm } from "../../components/event/editor/EventForm";
import { createEditor } from "../../components/event/editor/editor";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { FontSize, IconSize } from "../../theme/sizing";
import { Select } from "../../components/base/input/Select";
import { Divider } from "../../components/base/Divider";
import { Button } from "../../components/base/button/Button";
import { ScrollView } from "react-native";
import { IconDatabase } from "@tabler/icons-react-native";
import { OpenEvntSchema } from "@evnt/schema";
import { SafeAreaView } from "react-native-safe-area-context";

const validJson = (str: any): boolean => {
	if (typeof str !== "string") return false;
	try {
		const parsed = JSON.parse(str);
		return OpenEvntSchema.safeParse(parsed).success;
	} catch {
		return false;
	}
};

export default function NewEventPage() {
	const router = useRouter();
	const { data } = useLocalSearchParams();
	const [form, setForm] = useState<OpenEvnt>({ v: "0.1", name: {} });
	const editor = createEditor(form, setForm);

	useEffect(() => {}, [data, setForm]);

	useFocusEffect(
		useCallback(() => {
			if (data && typeof data === "string" && validJson(data)) {
				const parsed = JSON.parse(data);
				setForm(parsed);
			} else setForm({ v: "0.1", name: {} });
		}, [data]),
	);

	const save = useMutation({
		mutationFn: async (raw: string) => {
			console.log("Saving event", raw);
			return await EventsManager.addEventWithCache({
				source: { type: "local" },
				format: { type: "directory.evnt.event" },
				raw,
				parsed: editor.value,
				error: null,
			});
		},
		onSuccess: (id) => {
			router.push(`/event/${id}`);
			setForm({ v: "0.1", name: {} });
		},
	});

	return (
		<Box component={SafeAreaView} flex={1}>
			<Box component={ScrollView}>
				<Container size="sm" flex={1}>
					<Box py="md" flex={1}>
						<Box gap="md" flex={1} mb={300}>
							<Box>
								<Text fz={FontSize.h1} fw="bold">
									Create Event
								</Text>
							</Box>

							<Box>
								<Select
									label="Save to..."
									value="local"
									onChange={() => {}}
									data={["local"]}
									renderItem={(item) => {
										if (item === "local")
											return (
												<Box direction="row" gap="xs" align="center">
													<IconDatabase size={IconSize.sm} />
													<Text fz={FontSize.sm}>This Device</Text>
												</Box>
											);
										return item;
									}}
								/>
							</Box>

							<Divider />

							<EventForm editor={editor} />
						</Box>
					</Box>
				</Container>
			</Box>
			<Box pos="absolute" style={{ bottom: 0 }} w="100%">
				<Container size="sm" flex={1} pb="md">
					<Button
						variant="primary"
						w="100%"
						loading={save.isPending}
						onPress={() => save.mutate(JSON.stringify(editor.value))}
					>
						{save.isPending ? "Saving…" : "Save"}
					</Button>
				</Container>
			</Box>
		</Box>
	);
}
