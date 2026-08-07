import { useEffect, useState } from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useMutation } from "@tanstack/react-query";
import { EventResolver, EventSourceRegistry } from "@vantage/core";
import { createEditor, Editor } from "@/components/event/editor/editor";
import { EventForm } from "@/components/event/editor/EventForm";
import { Box } from "@/components/base/Box";
import { Text } from "@/components/base/Text";
import { Button } from "@/components/base/button/Button";
import { Colors } from "@/theme/colors";
import { Container } from "@/components/base/Container";
import { FontSize, IconSize } from "@/theme/sizing";
import { OpenEvnt } from "@evnt/types";
import { EmptyState } from "../../../components/base/EmptyState";
import { IconAlertTriangle } from "@tabler/icons-react-native";
import { ActionBackButton } from "../../../components/app/ActionBackButton";
import Animated from "react-native-reanimated";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";
import { Divider } from "../../../components/base/Divider";
import { useKeyboardHeight } from "../../../hooks/useKeyboardHeight";
import { Card } from "../../../components/base/Card";
import { useCanEditEvent } from "../../../hooks/useCanEditEvent";

export default function EditEventPage() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const router = useRouter();
	const insets = useSafeAreaInsets();
	const keyboardHeight = useKeyboardHeight();
	const [resolved, setResolved] = useState<Vantage.ResolvedEvent | null>(null);
	const [form, setForm] = useState<OpenEvnt | null>(null);
	const editor = createEditor(form, setForm);
	const canEdit = useCanEditEvent(resolved);

	const [error, setError] = useState<Error | null>(null);

	const handleLoadEvent = async () => {
		if (!id) throw new Error("No event ID provided");
		const resolved = await EventResolver.selectFromDatabase(
			EventResolver.new({ id: id as Vantage.EventId }),
		);
		if (!resolved) throw new Error("Event not found");
		if (resolved.format.type !== "directory.evnt.event")
			throw new Error("Cannot edit event of format " + resolved.format.type);
		if (resolved.error) throw new Error(resolved.error.message);
		if (!resolved.data) throw new Error("Event has no parsed data");
		setResolved(resolved);
		setForm(resolved.data);
	};

	const handleTryLoadEvent = async () => {
		try {
			setError(null);
			await handleLoadEvent();
		} catch (err) {
			setError(err as Error);
		}
	};

	useEffect(() => {
		handleTryLoadEvent();
	}, [id]);

	const save = useMutation({
		mutationFn: async () => {
			if (!editor.value) throw new Error("No event data to save");
			if (!resolved?.source) throw new Error("No event source to save to");
			if (!resolved?.id) throw new Error("Events must have an ID to be edited");
			const data: OpenEvnt = { ...editor.value, $type: "directory.evnt.event" };
			const source = EventSourceRegistry.get(resolved.source.type);
			if (!source) throw new Error("No source handler for " + resolved.source.type);
			if (!source.edit) throw new Error("Source " + resolved.source.type + " is not editable");
			await source.edit({ id: resolved.id, source: resolved.source, data });
		},
		onSuccess: () => {
			if (router.canGoBack()) router.back();
			else router.push(`/event/${id}`);
		},
	});

	if (!canEdit)
		return (
			<EmptyState
				fill
				icon={<IconAlertTriangle size={IconSize.xl} color={Colors.TextDimmed} />}
				message="You cannot edit this event"
				action={
					<Box gap="md" direction="row">
						<Button onPress={handleTryLoadEvent}>Retry</Button>
						<Button onPress={() => router.back()}>Go Back</Button>
					</Box>
				}
			/>
		);

	if (error)
		return (
			<EmptyState
				fill
				icon={<IconAlertTriangle size={IconSize.xl} color={Colors.TextDimmed} />}
				message={error?.message}
				action={
					<Box gap="md" direction="row">
						<Button onPress={handleTryLoadEvent}>Retry</Button>
						<Button onPress={() => router.back()}>Go Back</Button>
					</Box>
				}
			/>
		);

	if (!form) return <EmptyState fill loading message="Loading..." />;

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
										Edit Event
									</Text>
								</Box>

								{!canEdit && (
									<Card>
										<Text c={Colors.Yellow}>
											You cannot edit this event. You must be signed in to the account that created
											it.
										</Text>
									</Card>
								)}

								{save.error && (
									<Card>
										<Text c={Colors.Yellow}>Failed to save event: {save.error.toString()}</Text>
									</Card>
								)}

								<Divider />

								{editor.value && <EventForm editor={editor as Editor<OpenEvnt>} />}
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
