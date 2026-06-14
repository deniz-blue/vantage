import {
	TextInput as RNTextInput,
	type TextInputProps as RNTextInputProps,
} from "react-native";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { Sizing } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";
import { Colors } from "../../theme/colors";

export interface TextInputProps
	extends Omit<RNTextInputProps, "placeholderTextColor">,
		Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {}

export const TextInput = ({
	label,
	description,
	error,
	required,
	style,
	...rest
}: TextInputProps) => {
	return (
		<InputWrapper
			label={label}
			description={description}
			error={error}
			required={required}
		>
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
		</InputWrapper>
	);
};
