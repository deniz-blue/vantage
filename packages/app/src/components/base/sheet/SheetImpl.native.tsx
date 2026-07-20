import { Colors } from "../../../theme/colors";
import { Radius } from "../../../theme/sizing";
import { Box } from "../Box";
import type { SheetImplProps } from "./SheetImpl";
import { ModalBottomSheet } from "@swmansion/react-native-bottom-sheet";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Pressable, BackHandler } from "react-native";
import { useEffect } from "react";
import Animated, { useAnimatedStyle } from "react-native-reanimated";
import { useKeyboardHeight } from "../../../hooks/useKeyboardHeight";

export const SheetImpl = ({ open, onClose, children }: SheetImplProps) => {
	const insets = useSafeAreaInsets();
	const keyboardHeight = useKeyboardHeight(!open);
	const keyboardStyle = useAnimatedStyle(() => ({
		height: keyboardHeight.value,
	}));

	useEffect(() => {
		if (!open) return;
		const sub = BackHandler.addEventListener("hardwareBackPress", () => {
			onClose();
			return true;
		});
		return () => sub.remove();
	}, [open]);

	return (
		<ModalBottomSheet
			detents={[0, "content"]}
			index={open ? 1 : 0}
			nativeOverlay
			onIndexChange={(index) => {
				if (index === 0) onClose();
			}}
			scrimColor="rgba(0,0,0,0.5)"
			surface={
				<Box
					absoluteFill
					bg={Colors.Background}
					style={{ borderTopLeftRadius: Radius.lg, borderTopRightRadius: Radius.lg }}
				/>
			}
		>
			<Box align="center" w="100%">
				<Box
					h={8}
					w={80}
					my="sm"
					radius={Radius.md}
					bg={Colors.BackgroundLight}
					component={Pressable}
					onPress={onClose}
					accessible
					accessibilityLabel="Close bottom sheet"
					accessibilityHint="Double tap to close bottom sheet"
					accessibilityRole="button"
				/>
			</Box>
			{children}
			<Animated.View style={keyboardStyle} />
			<Box h={insets.bottom} />
		</ModalBottomSheet>
	);
};

export const KeyboardPadding = () => {};
