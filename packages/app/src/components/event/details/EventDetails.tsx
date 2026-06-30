import { useWindowDimensions, ScrollView, RefreshControl } from "react-native";
import { Box } from "../../base/Box";
import { EventDetailsError } from "./EventDetailsError";
import { EventDetailsBanner } from "./EventDetailsBanner";
import { EventDetailsInstanceList } from "./EventDetailsInstanceList";
import { EventDetailsActions } from "./EventDetailsActions";
import { EventDetailsLinks } from "./EventDetailsLinks";
import { EventDetailsRichtext } from "./EventDetailsRichtext";
import { EventDetailsSource } from "./EventDetailsSource";
import { EventDetailsImport } from "./EventDetailsImport";

const WIDE_BREAKPOINT = 768;

export const EventDetails = ({
	loading,
	onRefresh,
}: {
	loading?: boolean;
	onRefresh?: () => void;
}) => {
	const { width: screenWidth } = useWindowDimensions();
	const isWide = screenWidth >= WIDE_BREAKPOINT;

	const main = (
		<Box gap="md">
			<EventDetailsError />
			<EventDetailsImport />
			<EventDetailsInstanceList />
			<EventDetailsRichtext />
		</Box>
	);

	const sidebar = (
		<Box gap="md">
			<EventDetailsLinks />
			<EventDetailsSource />
		</Box>
	);

	return (
		<Box
			component={ScrollView}
			flex={1}
			stickyHeaderIndices={[0]}
			refreshControl={onRefresh && (
				<RefreshControl
					refreshing={loading ?? false}
					onRefresh={onRefresh}
				/>
			)}
		>
			<EventDetailsBanner loading={loading} />
			<Box gap="md" p="md">
				<EventDetailsActions />
				{isWide ? (
					<Box direction="row" gap="md">
						<Box flex={2}>{main}</Box>
						<Box flex={1}>{sidebar}</Box>
					</Box>
				) : (
					<>
						{main}
						{sidebar}
					</>
				)}
			</Box>
		</Box >
	);
};
