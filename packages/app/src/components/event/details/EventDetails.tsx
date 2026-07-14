import { useWindowDimensions } from "react-native";
import { Box } from "../../base/Box";
import { EventDetailsError } from "./EventDetailsError";
import { EventDetailsBanner, useBannerAnimatedHeight } from "./EventDetailsBanner";
import { EventDetailsInstanceList } from "./EventDetailsInstanceList";
import { EventDetailsActions } from "./EventDetailsActions";
import { EventDetailsLinks } from "./EventDetailsLinks";
import { EventDetailsRichtext } from "./EventDetailsRichtext";
import { EventDetailsSource } from "./EventDetailsSource";
import { EventDetailsImport } from "./EventDetailsImport";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import Animated, {
	useAnimatedScrollHandler,
	useAnimatedStyle,
	useSharedValue,
} from "react-native-reanimated";
import { memo } from "react";

const WIDE_BREAKPOINT = 768;

export const EventDetails = memo(({ loading }: { loading?: boolean; onRefresh?: () => void }) => {
	const insets = useSafeAreaInsets();

	const scrollY = useSharedValue(0);
	const bannerHeight = useSharedValue(0);

	const scrollHandler = useAnimatedScrollHandler((event) => {
		scrollY.value = event.contentOffset.y;
	});

	const animatedHeight = useSharedValue(0);
	useBannerAnimatedHeight(animatedHeight);

	const spacerStyle = useAnimatedStyle(() => ({
		height: bannerHeight.value + animatedHeight.value,
	}));

	return (
		<Box flex={1}>
			<Box absoluteFill style={{ zIndex: 1, pointerEvents: "box-none" }}>
				<EventDetailsBanner
					loading={loading}
					scrollY={scrollY}
					animatedHeight={animatedHeight}
					onHeaderLayout={(e) => (bannerHeight.value = e.nativeEvent.layout.height)}
				/>
			</Box>
			<Box
				component={Animated.ScrollView}
				flex={1}
				onScroll={scrollHandler}
				scrollEventThrottle={16}
				// TODO: custom scrollbar
				showsVerticalScrollIndicator={false}
				gap={0}
			>
				<Animated.View style={spacerStyle} />
				<EventDetailsInnerLayout />
				<Box h={200 + insets.bottom} />
			</Box>
		</Box>
	);
});

export const EventDetailsInnerLayout = memo(() => {
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
	);
});
