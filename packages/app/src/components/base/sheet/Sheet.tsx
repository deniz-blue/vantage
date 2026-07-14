import { PropsWithChildren } from "react";
import { Spacing } from "../Box";
import { useHistoryBack } from "../../../hooks/useHistoryBack";
import { ScrollView, ScrollViewProps } from "react-native";
import { SheetImpl } from "./SheetImpl";

export const HANDLE_BAR_HEIGHT = 28;

export const Sheet = ({
	children,
	onClose,
	open,
}: PropsWithChildren<{
	open: boolean;
	onClose: () => void;
	scrollable?: boolean;
	p?: Spacing;
}>) => {
	useHistoryBack(open, onClose);

	return <SheetImpl open={open} onClose={onClose} children={children} />;
};

export const SheetScrollView = ({
	...props
}: PropsWithChildren<ScrollViewProps & { ref?: React.Ref<ScrollView> }>) => {
	return <ScrollView {...props} />;
};
