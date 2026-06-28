import { useCallback, useState, type ReactNode } from "react";
import {
	TextInput as RNTextInput,
	type TextInputProps as RNTextInputProps,
} from "react-native";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { ControlHeight, FontSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { Colors } from "../../../theme/colors";
import { InputBase } from "./InputBase";

const INPUT_SIZES = {
	sm: { h: ControlHeight.sm, fz: FontSize.xs },
	md: { h: ControlHeight.md, fz: FontSize.sm },
	lg: { h: ControlHeight.lg, fz: FontSize.md },
} as const;

export interface TextInputProps
	extends Omit<RNTextInputProps, "placeholderTextColor">,
	Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
	size?: keyof typeof INPUT_SIZES;
	leftSection?: ReactNode;
	rightSection?: ReactNode;
}

export const TextInput = ({
	label,
	description,
	error,
	required,
	size = "md",
	leftSection,
	rightSection,
	style,
	...rest
}: TextInputProps) => {
	const inputSize = INPUT_SIZES[size];
	const [focused, setFocused] = useState(false);

	const handleFocus = useCallback(
		(e: any) => {
			setFocused(true);
			rest.onFocus?.(e);
		},
		[rest.onFocus],
	);

	const handleBlur = useCallback(
		(e: any) => {
			setFocused(false);
			rest.onBlur?.(e);
		},
		[rest.onBlur],
	);

	const input = (
		<RNTextInput
			placeholderTextColor={Colors.TextDimmed}
			style={[
				{
					flex: 1,
					color: Colors.Text,
					backgroundColor: "transparent",
					paddingVertical: Spacing.xs,
					fontSize: inputSize.fz,
					fontFamily: "Lexend",
					outlineWidth: 0,
				},
				style,
			]}
			{...rest}
			onFocus={handleFocus}
			onBlur={handleBlur}
		/>
	);

	return (
		<InputWrapper
			label={label}
			description={description}
			error={error}
			required={required}
		>
			<InputBase
				focused={focused}
				size={size}
				gap={leftSection || rightSection ? Spacing.sm : undefined}
				pl={!leftSection ? Spacing.sm : undefined}
				pr={!rightSection ? Spacing.sm : undefined}
				style={[
					{
						borderWidth: 1,
						borderColor: error ? "#f44336" : "transparent",
					},
				]}
			>
				{leftSection}
				{input}
				{rightSection}
			</InputBase>
		</InputWrapper>
	);
};
