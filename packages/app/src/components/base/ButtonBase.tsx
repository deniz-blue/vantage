import { Pressable } from "react-native";
import Animated, { useAnimatedStyle, useSharedValue, withTiming } from "react-native-reanimated";
import { ComponentProps } from "react";

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export interface ButtonBaseProps extends Omit<
	ComponentProps<typeof AnimatedPressable>,
	"onPressIn" | "onPressOut" | "key"
> {}

export const ButtonBase = ({ style, ...props }: ButtonBaseProps) => {
	const opacity = useSharedValue(1);

	const animatedStyle = useAnimatedStyle(() => ({
		opacity: opacity.value,
	}));

	const handlePressIn = () => {
		if (props.disabled || !props.onPress) return;
		opacity.value = withTiming(0.4, { duration: 100 });
	};

	const handlePressOut = () => {
		opacity.value = withTiming(1, { duration: 150 });
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
