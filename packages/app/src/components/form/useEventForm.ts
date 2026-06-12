import { useCallback, useState } from "react";
import { Alert } from "react-native";
import { useRouter } from "expo-router";
import { produce } from "immer";
import type { Draft } from "immer";
import type { OpenEvnt } from "@evnt/types";
import { EventsManager } from "@vantage/core";

// === Helpers ===

function defaultEvent(): OpenEvnt {
	return { v: "0.1", name: { en: "" } };
}

// === Hook ===

export interface UseEventFormReturn {
	event: OpenEvnt;
	/** Immer-style mutable draft updater. */
	update: (recipe: (draft: Draft<OpenEvnt>) => void) => void;
	canSave: boolean;
	handleSave: () => Promise<void>;
}

const useFormState = (initialData?: OpenEvnt) => {
	const [event, setEvent] = useState<OpenEvnt>(() => initialData ?? defaultEvent());

	const update = useCallback((recipe: (draft: Draft<OpenEvnt>) => void) => {
		setEvent((prev) => produce(prev, recipe));
	}, []);

	const canSave = (event.name?.en ?? "").trim().length > 0;

	return { event, update, canSave };
};

export const useEventForm = (initialData?: OpenEvnt): UseEventFormReturn => {
	const router = useRouter();
	const { event, update, canSave } = useFormState(initialData);

	const handleSave = useCallback(async () => {
		if (!canSave) return;

		EventsManager.addEventWithCache({
			format: { type: "directory.evnt.event" },
			source: { type: "local" },
			raw: JSON.stringify(event),
			parsed: event,
			error: null,
		}).then(() => {
			router.push("/(tabs)/list");
		}).catch((err) => {
			console.error("Failed to save event:", err);
			Alert.alert("Error", "Failed to save event. Check console for details.");
		});
	}, [event, canSave, router]);

	return { event, update, canSave, handleSave };
};

export const useEditEventForm = (
	eventId: string | undefined,
	initialData?: OpenEvnt,
): UseEventFormReturn => {
	const router = useRouter();
	const { event, update, canSave } = useFormState(initialData);

	const handleSave = useCallback(async () => {
		if (!canSave || !eventId) return;

		EventsManager.updateEventCache(eventId as Vantage.EventId, {
			raw: JSON.stringify(event),
			parsed: event,
			error: null,
		}).then(() => {
			router.back();
		}).catch((err) => {
			console.error("Failed to save event:", err);
			Alert.alert("Error", "Failed to save event. Check console for details.");
		});
	}, [event, canSave, eventId, router]);

	return { event, update, canSave, handleSave };
};
