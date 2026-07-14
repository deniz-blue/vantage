import { KeyboardAvoidingView } from "react-native";
import { Colors } from "../../../theme/colors";
import { Radius } from "../../../theme/sizing";
import { Box } from "../Box";
import type { SheetImplProps } from "./SheetImpl";
import { ModalBottomSheet } from "@swmansion/react-native-bottom-sheet";
import { SafeAreaView } from "react-native-safe-area-context";
import { Pressable } from "react-native";

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
				<SafeAreaView>
					<Box>
						<Box align="center" w="100%">
							<Box
								h={8}
								w={80}
								my="sm"
								radius={Radius.md}
								bg={Colors.BackgroundLight}
								component={Pressable}
								onPress={onClose}
								accessible
								accessibilityLabel="Close bottom sheet"
								accessibilityHint="Double tap to close bottom sheet"
								accessibilityRole="button"
							/>
						</Box>
						<Box w="100%">{children}</Box>
					</Box>
				</SafeAreaView>
			</KeyboardAvoidingView>
		</ModalBottomSheet>
	);
};
