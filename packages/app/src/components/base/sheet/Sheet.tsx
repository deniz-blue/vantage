import { PropsWithChildren, useImperativeHandle, useRef } from "react";
import { ScrollView } from "react-native";
import { TrueSheet } from "@lodev09/react-native-true-sheet";
import { Colors } from "../../../theme/colors";
import { Spacing } from "../../../theme/spacing";

export interface SheetRef {
	present: () => void;
	dismiss: () => void;
}

export interface SheetProps {
	ref: React.Ref<SheetRef>;
	scrollable?: boolean;
	withPadding?: boolean;
	onDidPresent?: () => void;
	onDidDismiss?: () => void;
	header?: React.ComponentType<any>;
	footer?: React.ComponentType<any>;
}

export const Sheet = ({
	children,
	onDidDismiss,
	onDidPresent,
	scrollable,
	withPadding = !scrollable,
	ref,
	header,
	footer,
}: PropsWithChildren<SheetProps>) => {
	const sheet = useRef<TrueSheet>(null);

	useImperativeHandle(
		ref,
		() => ({
			present: () => sheet.current?.present(),
			dismiss: () => sheet.current?.dismiss(),
		}),
		[sheet],
	);

	return (
		<TrueSheet
			ref={sheet}
			detents={["auto"]}
			onDidDismiss={onDidDismiss}
			onDidPresent={onDidPresent}
			backgroundColor={Colors.Background}
			grabberOptions={{ height: 4, topMargin: 16, color: Colors.BackgroundLight }}
			scrollable={scrollable}
			style={{
				paddingTop: (header ? 0 : 16) + (scrollable ? 0 : 20),
				paddingLeft: withPadding ? Spacing.sm : 0,
				paddingRight: withPadding ? Spacing.sm : 0,
				paddingBottom: withPadding ? Spacing.sm : 0,
			}}
			headerStyle={{
				marginTop: 36,
			}}
			header={header}
			footer={footer}
		>
			{children}
		</TrueSheet>
	);
};

export const SheetScrollView = ScrollView;
