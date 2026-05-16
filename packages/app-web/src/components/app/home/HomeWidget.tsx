import { THomeWidget } from "./useHomeStore";
import { WidgetUpcomingEvents } from "./widgets/WidgetUpcomingEvents";
import { WidgetWttrIn } from "./widgets/WidgetWttrIn";

export const HomeWidget = ({
	widget,
}: {
	widget: THomeWidget;
}) => {
	switch (widget.$type) {
		case "vantage.events.upcoming":
			return <WidgetUpcomingEvents />;
		case "wttr.in":
			return <WidgetWttrIn />;
		default:
			return null;
	}
};
