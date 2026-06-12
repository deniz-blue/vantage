import {
	TextInput as RNTextInput,
	type TextInputProps as RNTextInputProps,
	View,
} from "react-native";
import { Text } from "./Text";
import { Sizing } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";
import { Colors } from "../../theme/colors";

export interface TextInputProps extends Omit<RNTextInputProps, "placeholderTextColor"> {
	label?: string;
	error?: string;
}

export const TextInput = ({
	label,
	error,
	style,
	...rest
}: TextInputProps) => {
	return (
		<View style={{ gap: 6 }}>
			{label && (
				<Text style={{ fontSize: 13, color: Colors.TextDimmed }}>
					{label}
				</Text>
			)}

			<RNTextInput
				placeholderTextColor={Colors.TextDimmed}
				style={[
					{
						backgroundColor: Colors.BackgroundLight,
						color: Colors.Text,
						borderRadius: Spacing.Radius,
						paddingHorizontal: Sizing.inputPaddingH,
						paddingVertical: Sizing.inputPaddingV,
						fontSize: Sizing.inputFontSize,
						borderWidth: 1,
						borderColor: error ? "#f44336" : "transparent",
					},
					style,
				]}
				{...rest}
			/>

			{error && (
				<Text style={{ fontSize: 12, color: "#f44336" }}>
					{error}
				</Text>
			)}
		</View>
	);
};
