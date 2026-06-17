import { useCallback, useState, type ReactNode } from "react";
import {
	TextInput as RNTextInput,
	type TextInputProps as RNTextInputProps,
} from "react-native";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { Box } from "./Box";
import { FontSize, Radius } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";
import { Colors } from "../../theme/colors";

export interface TextInputProps
	extends Omit<RNTextInputProps, "placeholderTextColor">,
	Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
	leftSection?: ReactNode;
	rightSection?: ReactNode;
}

export const TextInput = ({
	label,
	description,
	error,
	required,
	leftSection,
	rightSection,
	style,
	...rest
}: TextInputProps) => {
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
					fontSize: FontSize.md,
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
			<Box
				direction="row"
				align="center"
				gap={leftSection || rightSection ? Spacing.sm : undefined}
				pl={!leftSection ? Spacing.sm : undefined}
				pr={!rightSection ? Spacing.sm : undefined}
				style={[
					{
						backgroundColor: Colors.BackgroundInput,
						borderRadius: Radius.Default,
						borderWidth: 1,
						borderColor: error ? "#f44336" : "transparent",
						outlineWidth: focused ? 2 : 0,
						outlineStyle: "solid",
						outlineColor: Colors.Primary,
					},
				]}
			>
				{leftSection}
				{input}
				{rightSection}
			</Box>
		</InputWrapper>
	);
};
