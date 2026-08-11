import { EventsLink } from "@vantage/core";
import { useRouter } from "expo-router";
import { useEffect } from "react";
import { Platform } from "react-native";

export const useWebEventsLinkHandler = () => {
	const router = useRouter();

	useEffect(() => {
		if (Platform.OS !== "web") return;
		const url = new URL(window.location.href);
		if (url.pathname !== "/") return;
		const intentData = EventsLink.parseIntent(url);
		if (intentData) {
			const target = new URLSearchParams();
			for (const [key, value] of Object.entries(intentData)) {
				if (value) target.set(key, value);
			}
			if (target.size > 0) router.push(`/event?${target.toString()}`);
		}
	}, []);
};
