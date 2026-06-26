import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import {
	BottomSheetModal,
	BottomSheetScrollView,
	BottomSheetBackdrop,
	type BottomSheetBackdropProps,
	type BottomSheetBackgroundProps,
} from "@gorhom/bottom-sheet";

import { Box } from "./Box";
import { Colors } from "../../theme/colors";
import { useHistoryBack } from "../../hooks/useHistoryBack";
import { useContextBridge } from "../../internal/react-context-bridge";

export const HANDLE_BAR_HEIGHT = 28;

interface SheetProps {
	children: ReactNode;
	open: boolean;
	onClose: () => void;
	/** Sheet height as a fraction of screen height (0–1). Default: 0.7. */
	height?: number;
	/** Whether the sheet wraps content in a ScrollView. Default: true. */
	scrollable?: boolean;
	keyboardShouldPersistTaps?: "always" | "never" | "handled";
}

export const Sheet = ({
	children,
	open,
	onClose,
	height: heightRatio = 0.7,
	scrollable = true,
	keyboardShouldPersistTaps,
}: SheetProps) => {
	const sheetRef = useRef<BottomSheetModal>(null);
	const userDismissed = useRef(false);

	useHistoryBack(open, onClose);

	const snapPoints = useMemo(() => {
		const pct = Math.round(heightRatio * 100);
		return [`${pct}%`, "100%"];
	}, [heightRatio]);

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
				<Box
					w={36}
					h={4}
					radius={2}
					bg={Colors.TextDimmed}
				/>
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
			snapPoints={snapPoints}
			onDismiss={handleDismiss}
			enablePanDownToClose
			animateOnMount
			backgroundComponent={renderBackground}
			handleComponent={renderHandle}
			backdropComponent={renderBackdrop}
		>
			<ContextBridge>
				{scrollable ? (
					<BottomSheetScrollView
						keyboardShouldPersistTaps={
							keyboardShouldPersistTaps ?? "never"
						}
					>
						{children}
						<Box h={32} />
					</BottomSheetScrollView>
				) : (
					children
				)}
			</ContextBridge>
		</BottomSheetModal>
	);
};
