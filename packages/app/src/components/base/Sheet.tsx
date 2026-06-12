import { useCallback, useEffect, useRef, useState } from "react";
import {
	Modal,
	TouchableOpacity,
	Animated,
	View,
	useWindowDimensions,
} from "react-native";
import type { ReactNode } from "react";
import { Colors } from "../../theme/colors";

const HANDLE_HEIGHT = 28;
const WIDE_BREAKPOINT = 640;
const MAX_SHEET_WIDTH = 420;

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
	const { height: screenHeight, width: screenWidth } = useWindowDimensions();
	const isWide = screenWidth >= WIDE_BREAKPOINT;

	const sheetHeight = Math.max(
		screenHeight * 0.3,
		Math.min(screenHeight * 0.9, screenHeight * heightRatio),
	);

	const slideAnim = useRef(new Animated.Value(1)).current;
	const isVisible = useRef(false);
	const didPushState = useRef(false);

	useEffect(() => {
		if (open && !isVisible.current) {
			isVisible.current = true;
			setRendered(true);
			slideAnim.setValue(1);
			Animated.timing(slideAnim, {
				toValue: 0,
				duration: 300,
				useNativeDriver: true,
			}).start();

			// Push history state on web so browser back closes the sheet
			if (typeof window !== "undefined" && window.history?.pushState) {
				window.history.pushState(null, "", window.location.href);
				didPushState.current = true;
			}
		} else if (!open && isVisible.current) {
			isVisible.current = false;

			// Pop pushed history state
			if (didPushState.current && typeof window !== "undefined" && window.history?.state) {
				window.history.back();
				didPushState.current = false;
			}

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

	// Listen for popstate (browser back) on web
	useEffect(() => {
		if (!rendered || typeof window === "undefined") return;

		const handlePopState = () => {
			if (isVisible.current) {
				onClose();
			}
		};

		window.addEventListener("popstate", handlePopState);
		return () => window.removeEventListener("popstate", handlePopState);
	}, [rendered, onClose]);

	const close = useCallback(() => {
		onClose();
	}, [onClose]);

	if (!rendered) return null;

	const translateY = slideAnim.interpolate({
		inputRange: [0, 1],
		outputRange: [0, sheetHeight + HANDLE_HEIGHT],
	});

	return (
		<Modal
			visible
			transparent
			animationType="fade"
			onRequestClose={close}
			statusBarTranslucent
		>
			<View style={{ flex: 1 }}>
				{/* Backdrop — covers everything */}
				<TouchableOpacity
					style={{ flex: 1, backgroundColor: "#00000066" }}
					activeOpacity={1}
					onPress={close}
				/>

				{/* Sheet card — overlaid on top of backdrop */}
				{isWide ? (
					<Animated.View
						style={{
							position: "absolute",
							top: 0,
							bottom: 0,
							justifyContent: "center",
							alignSelf: "center",
							width: MAX_SHEET_WIDTH,
							maxHeight: sheetHeight,
							backgroundColor: Colors.Background,
							borderRadius: 16,
							overflow: "hidden",
							transform: [{ translateY }],
						}}
					>
						<View style={{ paddingVertical: 8, alignItems: "center" }}>
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
				) : (
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
							transform: [{ translateY }],
						}}
					>
						<View style={{ paddingVertical: 8, alignItems: "center" }}>
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
				)}
			</View>
		</Modal>
	);
};
