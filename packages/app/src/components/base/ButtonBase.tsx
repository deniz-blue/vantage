import { Pressable } from "react-native";
import Animated, { useAnimatedStyle, useSharedValue, withTiming } from "react-native-reanimated";
import { ComponentProps } from "react";
import { GestureResponderEvent } from "react-native";

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export interface ButtonBaseProps extends Omit<
	ComponentProps<typeof AnimatedPressable>,
	"onPressIn" | "onPressOut" | "key"
> {
	onPressIn?: (e: GestureResponderEvent) => void;
	onPressOut?: (e: GestureResponderEvent) => void;
}

export const ButtonBase = ({ style, onPressIn, onPressOut, ...props }: ButtonBaseProps) => {
	const opacity = useSharedValue(1);

	const animatedStyle = useAnimatedStyle(() => ({
		opacity: opacity.value,
	}));

	const handlePressIn = (e: GestureResponderEvent) => {
		onPressIn?.(e);
		if (props.disabled || !props.onPress) return;
		opacity.value = withTiming(0.4, { duration: 100 });
	};

	const handlePressOut = (e: GestureResponderEvent) => {
		opacity.value = withTiming(1, { duration: 150 });
		onPressOut?.(e);
	};

	return (
		<AnimatedPressable
			role="button"
			accessibilityRole="button"
			onPressIn={handlePressIn}
			onPressOut={handlePressOut}
			style={[animatedStyle, style]}
			{...props}
		/>
	);
};
