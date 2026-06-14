import { type ReactNode } from "react";
import { View, Text as RNText } from "react-native";
import { Text, type TextProps } from "./Text";
import { Colors } from "../../theme/colors";

export interface InputWrapperProps {
	/** Label rendered above the input. */
	label?: ReactNode;
	/** Description shown below the label (hidden when `error` is set). */
	description?: ReactNode;
	/** Error message shown below the input. Replaces description visually. */
	error?: ReactNode;
	/** Adds a red asterisk after the label. */
	required?: boolean;
	labelProps?: Omit<TextProps, "children">;
	descriptionProps?: Omit<TextProps, "children">;
	errorProps?: Omit<TextProps, "children">;

	/** The actual input element(s). */
	children?: ReactNode;
}

/**
 * InputWrapper — label / description / error layout around any input child.
 *
 * Modeled after Mantine's Input.Wrapper:
 * - `label` + optional red asterisk when `required`
 * - `description` below the label (hidden when `error` is present)
 * - `error` below the children, replacing the description slot
 */
export const InputWrapper = ({
	label,
	description,
	error,
	required,
	labelProps,
	descriptionProps,
	errorProps,
	children,
}: InputWrapperProps) => {
	return (
		<View style={{ gap: 6 }}>
			{label && (
				<Text
					fz={13}
					c={Colors.TextDimmed}
					{...labelProps}
				>
					{label}
					{required && (
						<RNText style={{ color: Colors.Red }}>
							{" *"}
						</RNText>
					)}
				</Text>
			)}

			{!error && description && (
				<Text
					fz={12}
					c={Colors.TextDimmed}
					{...descriptionProps}
				>
					{description}
				</Text>
			)}

			{children}

			{error && (
				<Text
					fz={12}
					c={Colors.Red}
					{...errorProps}
				>
					{error}
				</Text>
			)}
		</View>
	);
};
