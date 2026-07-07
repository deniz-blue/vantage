import { PropsWithChildren, useEffect, useId } from "react";
import {
	BottomSheetPortal,
	useBottomSheetControl,
	CustomModalAdapter,
	useBottomSheetContext,
} from "react-native-bottom-sheet-stack";
import { GorhomSheetAdapter } from "react-native-bottom-sheet-stack/gorhom";
import { Box, ShorthandStyleProps } from "./Box";
import { Colors } from "../../theme/colors";
import {
	Animated,
	Pressable,
	ScrollView,
	ScrollViewProps,
	useWindowDimensions,
} from "react-native";
import { Breakpoints } from "../../theme/breakpoints";
import { Radius } from "../../theme/sizing";

export const HANDLE_BAR_HEIGHT = 28;

export const Sheet = ({
	children,
	onClose,
	open,
	scrollable,
	p = "md",
}: PropsWithChildren<{
	open: boolean;
	onClose: () => void;
	scrollable?: boolean;
	keyboardShouldPersistTaps?: "always" | "never" | "handled";
	p?: ShorthandStyleProps["p"];
}>) => {
	const id = useId();
	const control = useBottomSheetControl(id);
	const { width } = useWindowDimensions();
	const isWide = width >= Breakpoints.SheetModal;

	useEffect(() => {
		if (open) control.open();
		else control.close();
	}, [open]);

	return (
		<BottomSheetPortal id={id}>
			{isWide ? <SheetImplWide children={children} /> : <SheetImplNarrow children={children} />}
		</BottomSheetPortal>
	);
};

export const SheetScrollView = ({
	children,
	...props
}: PropsWithChildren<ScrollViewProps & { ref?: React.Ref<ScrollView> }>) => {
	return <ScrollView {...props}>{children}</ScrollView>;
};

export const SheetImplWide = ({ children }: PropsWithChildren) => {
	return (
		<CustomModalAdapter
			contentContainerStyle={{
				backgroundColor: "rgba(0,0,0,0.5)",
			}}
		>
			<Box
				component={Pressable}
				onPress={(e) => e.stopPropagation()}
				bg={Colors.Background}
				radius={Radius.Default}
				w="100%"
				style={{
					maxWidth: 480,
					maxHeight: "90%",
					overflow: "hidden",
					zIndex: 5,
				}}
			>
				{children}
			</Box>
		</CustomModalAdapter>
	);
};

export const SheetImplNarrow = ({ children }: PropsWithChildren) => {
	return (
		<GorhomSheetAdapter enablePanDownToClose animateOnMount>
			{children}
		</GorhomSheetAdapter>
	);
};
