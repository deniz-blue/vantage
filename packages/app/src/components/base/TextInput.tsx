import {
	TextInput as RNTextInput,
	type TextInputProps as RNTextInputProps,
} from "react-native";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { FontSize, Radius } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";
import { Colors } from "../../theme/colors";

export interface TextInputProps
	extends Omit<RNTextInputProps, "placeholderTextColor">,
	Pick<InputWrapperProps, "label" | "description" | "error" | "required"> { }

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
						borderRadius: Radius.Default,
						paddingHorizontal: Spacing.sm,
						paddingVertical: Spacing.xs,
						fontSize: FontSize.md,
						fontFamily: "Lexend",
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
