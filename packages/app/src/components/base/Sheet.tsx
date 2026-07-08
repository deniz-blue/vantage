import { PropsWithChildren, useCallback, useMemo } from "react";
import { Box, ShorthandStyleProps } from "./Box";
import { Colors } from "../../theme/colors";
import { ScrollView, ScrollViewProps, useWindowDimensions } from "react-native";
import { Breakpoints } from "../../theme/breakpoints";
import { Radius } from "../../theme/sizing";
import { Portal } from "react-native-teleport";
import BottomSheet, {
	BottomSheetBackdrop,
	BottomSheetBackdropProps,
} from "@gorhom/bottom-sheet";

export const Sheet = ({
	children,
	onClose,
	open,
	scrollable,
	keyboardShouldPersistTaps,
	p = "md",
}: PropsWithChildren<{
	open: boolean;
	onClose: () => void;
	scrollable?: boolean;
	keyboardShouldPersistTaps?: "always" | "never" | "handled";
	p?: ShorthandStyleProps["p"];
}>) => {
	const { width } = useWindowDimensions();
	const isWide = width >= Breakpoints.SheetModal;

	if (!open) return null;

	const child = scrollable ? (
		<SheetScrollView keyboardShouldPersistTaps={keyboardShouldPersistTaps}>
			<Box p={p}>{children}</Box>
		</SheetScrollView>
	) : (
		<Box p={p}>{children}</Box>
	);

	const props = { children: child, onClose };

	return (
		<Portal hostName="overlay">
			<Box absoluteFill pointerEvents="auto">
				{isWide ? <SheetImplWide {...props} /> : <SheetImplNarrow {...props} />}
			</Box>
		</Portal>
	);
};

export const SheetScrollView = ({
	children,
	...props
}: PropsWithChildren<ScrollViewProps & { ref?: React.Ref<ScrollView> }>) => {
	return <ScrollView {...props}>{children}</ScrollView>;
};

const SheetImplWide = ({ children, onClose }: PropsWithChildren<{
	onClose: () => void;
}>) => {
	return (
		<Box
			absoluteFill
			bg="rgba(0,0,0,0.5)"
			onStartShouldSetResponder={() => true}
			onResponderGrant={onClose}
		>
			<Box
				absoluteFill
				justify="center"
				align="center"
				pointerEvents="box-none"
			>
				<Box
					bg={Colors.Background}
					radius={Radius.Default}
					w="100%"
					style={{
						maxWidth: 480,
						maxHeight: "90%",
						overflow: "hidden",
					}}
					onStartShouldSetResponder={() => true}
					onResponderRelease={() => {}}
				>
					{children}
				</Box>
			</Box>
		</Box>
	);
};

const SheetImplNarrow = ({ children, onClose }: PropsWithChildren<{
	onClose: () => void;
}>) => {
	const snapPoints = useMemo(() => ["100%"], []);

	const handleChange = useCallback((index: number) => {
		if (index === -1) {
			onClose();
		}
	}, [onClose]);

	const renderBackdrop = useCallback((props: BottomSheetBackdropProps) => (
		<BottomSheetBackdrop
			{...props}
			disappearsOnIndex={-1}
			appearsOnIndex={0}
		/>
	), []);

	return (
		<BottomSheet
			index={0}
			snapPoints={snapPoints}
			enablePanDownToClose
			onChange={handleChange}
			backdropComponent={renderBackdrop}
		>
			{children}
		</BottomSheet>
	);
};
