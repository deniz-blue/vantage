import { KeyboardAvoidingView } from "react-native";
import { Colors } from "../../../theme/colors";
import { Radius } from "../../../theme/sizing";
import { Box } from "../Box";
import type { SheetImplProps } from "./SheetImpl";
import { ModalBottomSheet } from "@swmansion/react-native-bottom-sheet";
import { SafeAreaView } from "react-native-safe-area-context";

export const SheetImpl = ({ open, onClose, children }: SheetImplProps) => {
	return (
		<ModalBottomSheet
			detents={[0, "content"]}
			index={open ? 1 : 0}
			nativeOverlay
			onIndexChange={(index) => {
				if (index === 0) onClose();
			}}
			scrimColor="rgba(0,0,0,0.5)"
			surface={
				<Box
					absoluteFill
					bg={Colors.Background}
					style={{ borderTopLeftRadius: Radius.lg, borderTopRightRadius: Radius.lg }}
				/>
			}
		>
			<KeyboardAvoidingView behavior="padding">
				<SafeAreaView>{children}</SafeAreaView>
			</KeyboardAvoidingView>
		</ModalBottomSheet>
	);
};
