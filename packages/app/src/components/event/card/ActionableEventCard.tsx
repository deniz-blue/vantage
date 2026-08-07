import { useResolvedEvent } from "@vantage/core";
import { Fragment, memo, useCallback, useRef } from "react";
import { EventCard } from "./EventCard";
import { EventActionsSheet } from "../../app/EventActionsSheet";
import { SheetRef } from "../../base/sheet/Sheet";
import { useRouter } from "expo-router";

export const ActionableEventCard = memo(({ fill }: { fill?: boolean }) => {
	const sheet = useRef<SheetRef>(null);
	const router = useRouter();
	const resolved = useResolvedEvent();

	const onPress = useCallback(() => {
		if (resolved.id) router.push(`/event/${resolved.id}`);
	}, [router, resolved.id]);

	const onLongPress = useCallback(() => {
		sheet.current?.present();
	}, [sheet]);

	return (
		<Fragment>
			<EventCard onPress={onPress} onLongPress={onLongPress} fill={fill} />
			<EventActionsSheet sheet={sheet} />
		</Fragment>
	);
});
