import { PropsWithChildren, useCallback, useEffect, useRef, useState } from "react";
import {
	BottomSheetModal,
	BottomSheetScrollView,
	BottomSheetBackdrop,
	type BottomSheetBackdropProps,
	type BottomSheetBackgroundProps,
} from "@gorhom/bottom-sheet";
import { Box, Spacing } from "../Box";
import { Colors } from "../../../theme/colors";
import { useHistoryBack } from "../../../hooks/useHistoryBack";
import { FiberHandle, useContextBridge } from "../../../internal/react-context-bridge";
import { Animated, Modal, ScrollView, ScrollViewProps, useWindowDimensions } from "react-native";
import { Breakpoints } from "../../../theme/breakpoints";
import { Radius } from "../../../theme/sizing";
import { SafeAreaView } from "react-native-safe-area-context";
import { Pressable } from "react-native";

export const HANDLE_BAR_HEIGHT = 28;

export const Sheet = ({
	children,
	onClose,
	open,
	scrollable,
	p = "sm",
}: PropsWithChildren<{
	open: boolean;
	onClose: () => void;
	scrollable?: boolean;
	p?: Spacing;
}>) => {
	const { width } = useWindowDimensions();
	const isWide = width >= Breakpoints.SheetModal;

	useHistoryBack(open, onClose);

	const child = (
		<Box component={SafeAreaView} p={p} flex={scrollable ? undefined : 1}>
			{children}
		</Box>
	);

	if (isWide) {
		return (
			<SheetImplModal open={open} onClose={onClose} children={child} scrollable={scrollable} />
		);
	} else {
		return (
			<SheetImplBottomSheet
				open={open}
				onClose={onClose}
				scrollable={scrollable}
				children={child}
			/>
		);
	}
};

export const SheetScrollView = ({
	children,
	...props
}: PropsWithChildren<ScrollViewProps & { ref?: React.Ref<ScrollView> }>) => {
	const { width } = useWindowDimensions();
	const isWide = width >= Breakpoints.SheetModal;

	return isWide ? (
		<ScrollView {...props}>{children}</ScrollView>
	) : (
		<BottomSheetScrollView keyboardShouldPersistTaps="never" {...props}>
			{children}
		</BottomSheetScrollView>
	);
};

export const SheetImplModal = ({
	children,
	open,
	onClose,
	scrollable = true,
}: PropsWithChildren<{
	open: boolean;
	onClose: () => void;
	scrollable?: boolean;
}>) => {
	const fadeAnim = useRef(new Animated.Value(0)).current;
	const [visible, setVisible] = useState(false);

	useEffect(() => {
		if (open) {
			setVisible(true);
			Animated.timing(fadeAnim, {
				toValue: 1,
				duration: 200,
				useNativeDriver: true,
			}).start();
		} else if (visible) {
			Animated.timing(fadeAnim, {
				toValue: 0,
				duration: 150,
				useNativeDriver: true,
			}).start(() => setVisible(false));
		}
	}, [open, visible, fadeAnim]);

	return (
		<Modal visible={visible} onRequestClose={onClose} animationType="none" transparent>
			<Box component={Animated.View} style={{ opacity: fadeAnim }} flex={1}>
				<Box
					component={Pressable}
					onPress={onClose}
					justify="center"
					align="center"
					flex={1}
					bg="rgba(0,0,0,0.5)"
				>
					<Box
						component={Pressable}
						onPress={(e) => e.stopPropagation()}
						bg={Colors.Background}
						radius={Radius.Default}
						w="100%"
						my="md"
						style={{
							maxWidth: 480,
							maxHeight: "90%",
							overflow: "hidden",
						}}
					>
						{scrollable ? <SheetScrollView>{children}</SheetScrollView> : children}
					</Box>
				</Box>
			</Box>
		</Modal>
	);
};

export const SheetImplBottomSheet = ({
	open,
	onClose,
	children,
	scrollable = true,
}: PropsWithChildren<{
	open: boolean;
	scrollable?: boolean;
	onClose: () => void;
}>) => {
	const sheetRef = useRef<BottomSheetModal>(null);
	const userDismissed = useRef(false);

	const [mounted, setMounted] = useState(false);
	const ContextBridge = useContextBridge();

	const handleDismiss = useCallback(() => {
		userDismissed.current = true;
		onClose();
	}, [onClose]);

	const renderBackground = useCallback(
		(props: BottomSheetBackgroundProps) => (
			<Box
				style={[
					props.style,
					{
						backgroundColor: Colors.Background,
						borderTopLeftRadius: 16,
						borderTopRightRadius: 16,
						overflow: "hidden",
					},
				]}
			/>
		),
		[],
	);

	const renderHandle = useCallback(
		() => (
			<Box py={8} align="center" h={HANDLE_BAR_HEIGHT}>
				<Box w={36} h={4} radius={2} bg={Colors.TextDimmed} />
			</Box>
		),
		[],
	);

	const renderBackdrop = useCallback(
		(props: BottomSheetBackdropProps) => (
			<BottomSheetBackdrop
				{...props}
				appearsOnIndex={0}
				disappearsOnIndex={-1}
				pressBehavior="close"
			/>
		),
		[],
	);

	useEffect(() => {
		if (open && !mounted) {
			setMounted(true);
		} else if (mounted) {
			if (open) {
				userDismissed.current = false;
				setTimeout(() => {
					sheetRef.current?.present();
				}, 0);
			} else if (!userDismissed.current) {
				sheetRef.current?.dismiss();
			}
		}
	}, [open, mounted]);

	if (!mounted) return null;

	return (
		<BottomSheetModal
			ref={sheetRef}
			onDismiss={handleDismiss}
			enablePanDownToClose
			animateOnMount
			backgroundComponent={renderBackground}
			handleComponent={renderHandle}
			backdropComponent={renderBackdrop}
			stackBehavior="push"
		>
			<FiberHandle>
				<ContextBridge>
					{scrollable ? <SheetScrollView children={children} /> : children}
				</ContextBridge>
			</FiberHandle>
		</BottomSheetModal>
	);
};
