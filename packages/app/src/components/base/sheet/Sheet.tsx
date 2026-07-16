import { PropsWithChildren } from "react";
import { Spacing } from "../Box";
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
	return <SheetImpl open={open} onClose={onClose} children={children} />;
};

export const SheetScrollView = ({
	...props
}: PropsWithChildren<ScrollViewProps & { ref?: React.Ref<ScrollView> }>) => {
	return <ScrollView {...props} />;
};
