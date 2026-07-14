import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { FontSize } from "../../../theme/sizing";
import { useRouter } from "expo-router";
import { ActionIcon } from "../../base/button/ActionIcon";
import { IconArrowLeft } from "@tabler/icons-react-native";
import { ActivityIndicator, type LayoutChangeEvent } from "react-native";
import { Colors } from "../../../theme/colors";
import { SplashMediaComponent } from "@evnt/types";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { EventBackground } from "../EventBackground";
import Animated, {
	SharedValue,
	useAnimatedStyle,
	useSharedValue,
	withTiming,
} from "react-native-reanimated";
import { useEffect } from "react";

export const HEADER_MAX_HEIGHT = 80;

export const useEventSplashMedia = () => {
	const { data } = useResolvedEvent();

	const splash = data?.components?.find((c) => {
		if (c.$type !== "directory.evnt.component.splashMedia") return false;
		return (c as SplashMediaComponent).roles.includes("background");
	}) as SplashMediaComponent | undefined;

	return splash;
};

export const useBannerAnimatedHeight = () => {
	const splash = useEventSplashMedia();
	const animatedHeight = useSharedValue(0);

	useEffect(() => {
		animatedHeight.value = withTiming(splash ? HEADER_MAX_HEIGHT : 0, {
			duration: 300,
		});
	}, [splash]);

	return animatedHeight;
};

export const EventDetailsBanner = ({
	loading,
	scrollY,
	onHeaderLayout,
}: {
	loading?: boolean;
	scrollY: SharedValue<number>;
	onHeaderLayout?: (e: LayoutChangeEvent) => void;
}) => {
	const animatedHeight = useBannerAnimatedHeight();

	const style = useAnimatedStyle(() => {
		return { height: Math.max(0, animatedHeight.value - scrollY.value) };
	});

	return (
		<Box bg={Colors.Background}>
			<Animated.View style={style} />
			<EventBackground />
			<EventDetailsHeader loading={loading} onLayout={onHeaderLayout} />
		</Box>
	);
};

export const EventDetailsHeader = ({
	loading,
	onLayout,
}: {
	loading?: boolean;
	onLayout?: (e: LayoutChangeEvent) => void;
}) => {
	const { top } = useSafeAreaInsets();
	const router = useRouter();
	const { data } = useResolvedEvent();

	return (
		<Box onLayout={onLayout}>
			<Box h={top} />
			<Box
				direction="row"
				gap="sm"
				py="md"
				px="md"
				style={{
					shadowColor: "#000",
					shadowOffset: { width: 0, height: 2 },
					shadowOpacity: 0.15,
					shadowRadius: 6,
					elevation: 4,
				}}
			>
				<ActionIcon
					variant="subtle"
					onPress={() => (router.canGoBack() ? router.back() : router.push("/"))}
					size="auto"
				>
					<IconArrowLeft size={20} color={Colors.Text} />
				</ActionIcon>
				{loading && <ActivityIndicator />}
				<Box flex={1} gap={4}>
					<TransText
						fz={FontSize.h1}
						fw="bold"
						value={data?.name}
						fallback={
							<Text fz={16} fst="italic" c="TextDimmed">
								{loading ? "Loading…" : "Untitled"}
							</Text>
						}
					/>
					{data?.label && <TransText fz={FontSize.md} c="TextDimmed" value={data.label} />}
				</Box>
			</Box>
		</Box>
	);
};
