import { useEffect, useRef } from "react";
import { Animated, type ViewStyle } from "react-native";
import { Colors } from "../../theme/colors";

export interface SkeletonProps {
	w?: number | string;
	h?: number | string;
	radius?: number;
	style?: ViewStyle;
}

export const Skeleton = ({
	w = "100%",
	h = 16,
	radius = 4,
	style,
}: SkeletonProps) => {
	const opacity = useRef(new Animated.Value(0.3));

	useEffect(() => {
		const animation = Animated.loop(
			Animated.sequence([
				Animated.timing(opacity.current, {
					toValue: 0.7,
					duration: 800,
					useNativeDriver: true,
				}),
				Animated.timing(opacity.current, {
					toValue: 0.3,
					duration: 800,
					useNativeDriver: true,
				}),
			]),
		);
		animation.start();
		return () => animation.stop();
	}, []);

	return (
		<Animated.View
			style={[
				{
					width: w as any,
					height: h as any,
					borderRadius: radius,
					backgroundColor: Colors.BackgroundLight,
					opacity: opacity.current,
				},
				style,
			]}
		/>
	);
};
