import { useEffect, useState } from "react";
import { ScrollView } from "react-native";
import { useLocalSearchParams, Stack, useRouter } from "expo-router";
import { useMutation } from "@tanstack/react-query";
import { EventResolver, EventsManager } from "@vantage/core";
import { createEditor, Editor } from "@/components/event/editor/editor";
import { EventForm } from "@/components/event/editor/EventForm";
import { Box } from "@/components/base/Box";
import { Text } from "@/components/base/Text";
import { Button } from "@/components/base/Button";
import { Colors } from "@/theme/colors";
import { Container } from "@/components/base/Container";
import { FontSize } from "@/theme/sizing";
import { OpenEvnt } from "@evnt/types";

type State =
	| { kind: "loading" }
	| { kind: "error" }
	| { kind: "non-local" }
	| { kind: "loaded" };

export default function EditEventPage() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const router = useRouter();
	const [form, setForm] = useState<OpenEvnt | null>(null);
	const editor = createEditor(form, setForm);

	const [state, setState] = useState<State>({ kind: "loading" });

	useEffect(() => {
		let cancelled = false;

		(async () => {
			const resolved = await EventResolver.selectFromDatabase(
				EventResolver.new({ id: id as any }),
			);

			if (cancelled) return;

			if (!resolved.id || !resolved.data) {
				setState({ kind: "error" });
				return;
			}

			if (resolved.source.type !== "local") {
				setState({ kind: "non-local" });
				return;
			}

			setState({ kind: "loaded" });
			setForm(resolved.data);
		})();

		return () => { cancelled = true; };
	}, [id]);

	const save = useMutation({
		mutationFn: async () => {
			const raw = JSON.stringify(editor.value);
			await EventsManager.updateEventCache(id as any, {
				raw,
				parsed: editor.value,
				error: null,
			});
		},
		onSuccess: () => {
			router.back();
		},
	});

	if (state.kind === "non-local") {
		return (
			<Box flex={1} px="md" pt="md" align="center" justify="center">
				<Box mb="md">
					<Text ta="center">
						This event is from a remote source and can't be edited here yet.
					</Text>
				</Box>
				<Button onPress={() => router.back()}>Go Back</Button>
			</Box>
		);
	}

	if (state.kind === "error") {
		return (
			<Box flex={1} px="md" pt="md" align="center" justify="center">
				<Box mb="md">
					<Text ta="center">Could not load event.</Text>
				</Box>
				<Button onPress={() => router.back()}>Go Back</Button>
			</Box>
		);
	}

	if (state.kind === "loading") {
		return (
			<Box flex={1} px="md" pt="md">
				<Text>Loading event…</Text>
			</Box>
		);
	}

	return (
		<Box flex={1} bg={Colors.Background}>
			<Stack.Screen
				options={{
					headerStyle: { backgroundColor: Colors.BackgroundLight } as any,
					headerTintColor: Colors.Text,
					headerShadowVisible: false,
				}}
			/>

			<Box flex={1}>
				<Box component={ScrollView}>
					<Container size="sm" flex={1}>
						<Box py="md" flex={1}>
							<Box gap="md" flex={1} mb={300}>
								<Box>
									<Text fz={FontSize.h1} fw="bold">
										Edit Event
									</Text>
								</Box>

								{editor.value ? (
									<EventForm editor={editor as Editor<OpenEvnt>} />
								) : (
									<Text c="Yellow">No event data available.</Text>
								)}
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
							onPress={() => save.mutate()}
						>
							{save.isPending ? "Saving…" : "Save Changes"}
						</Button>
					</Container>
				</Box>
			</Box>
		</Box>
	);
}
