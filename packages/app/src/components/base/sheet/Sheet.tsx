import { PropsWithChildren } from "react";
import { Box, Spacing } from "../Box";
import { useHistoryBack } from "../../../hooks/useHistoryBack";
import { ScrollView, ScrollViewProps } from "react-native";
import { SheetImpl } from "./SheetImpl";

export const HANDLE_BAR_HEIGHT = 28;

export const Sheet = ({
	children,
	onClose,
	open,
	p = "sm",
}: PropsWithChildren<{
	open: boolean;
	onClose: () => void;
	scrollable?: boolean;
	p?: Spacing;
}>) => {
	useHistoryBack(open, onClose);

	const child = <Box p={p}>{children}</Box>;

	return <SheetImpl open={open} onClose={onClose} children={child} />;
};

export const SheetScrollView = ({
	...props
}: PropsWithChildren<ScrollViewProps & { ref?: React.Ref<ScrollView> }>) => {
	return <ScrollView {...props} />;
};
