import { useCallback, useEffect, useRef, useState } from "react";
import {
	Modal,
	TouchableOpacity,
	Animated,
	Dimensions,
	View,
} from "react-native";
import type { ReactNode } from "react";
import { Box } from "./Box";
import { Colors } from "../../theme/colors";

const SCREEN_HEIGHT = Dimensions.get("window").height;
const HANDLE_HEIGHT = 28;

export interface SheetProps {
	children: ReactNode;
	open: boolean;
	onClose: () => void;
	height?: number;
}

export const Sheet = ({
	children,
	open,
	onClose,
	height: heightRatio = 0.7,
}: SheetProps) => {
	const [rendered, setRendered] = useState(false);

	const sheetHeight = Math.max(
		SCREEN_HEIGHT * 0.3,
		Math.min(SCREEN_HEIGHT * 0.9, SCREEN_HEIGHT * heightRatio),
	);

	const slideAnim = useRef(new Animated.Value(1)).current;

	// Track animation state so we don't race
	const isVisible = useRef(false);

	useEffect(() => {
		if (open && !isVisible.current) {
			isVisible.current = true;
			setRendered(true);
			// Start closed (below screen), animate up
			slideAnim.setValue(1);
			Animated.timing(slideAnim, {
				toValue: 0,
				duration: 300,
				useNativeDriver: true,
			}).start();
		} else if (!open && isVisible.current) {
			isVisible.current = false;
			// Animate down, then unmount
			Animated.timing(slideAnim, {
				toValue: 1,
				duration: 200,
				useNativeDriver: true,
			}).start(() => {
				setRendered(false);
			});
		}
	}, [open, slideAnim]);

	const close = useCallback(() => {
		onClose();
	}, [onClose]);

	if (!rendered) return null;

	return (
		<Modal
			visible
			transparent
			animationType="fade"
			onRequestClose={close}
			statusBarTranslucent
		>
			<View style={{ flex: 1 }}>
				{/* Backdrop */}
				<TouchableOpacity
					style={{ flex: 1, backgroundColor: "#00000066" }}
					activeOpacity={1}
					onPress={close}
				/>

				{/* Sheet */}
				<Animated.View
					style={{
						position: "absolute",
						bottom: 0,
						left: 0,
						right: 0,
						height: sheetHeight,
						backgroundColor: Colors.Background,
						borderTopLeftRadius: 16,
						borderTopRightRadius: 16,
						overflow: "hidden",
						transform: [
							{
								translateY: slideAnim.interpolate({
									inputRange: [0, 1],
									outputRange: [0, sheetHeight + HANDLE_HEIGHT],
								}),
							},
						],
					}}
				>
					{/* Handle */}
					<View
						style={{
							paddingVertical: 8,
							alignItems: "center",
						}}
					>
						<View
							style={{
								width: 36,
								height: 4,
								borderRadius: 2,
								backgroundColor: Colors.TextDimmed,
							}}
						/>
					</View>

					{children}
				</Animated.View>
			</View>
		</Modal>
	);
};
