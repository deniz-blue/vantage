import { type ReactNode } from "react";
import { Text, type TextProps } from "../Text";
import { Colors } from "../../../theme/colors";
import { FontSize } from "../../../theme/sizing";
import { Box } from "../Box";

export interface InputWrapperProps {
	label?: ReactNode;
	description?: ReactNode;
	error?: ReactNode;
	required?: boolean;
	labelProps?: Omit<TextProps, "children">;
	descriptionProps?: Omit<TextProps, "children">;
	errorProps?: Omit<TextProps, "children">;
	children?: ReactNode;
}

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
	const labelNode = label && (
		<Text
			fz={FontSize.sm}
			fw="600"
			{...labelProps}
		>
			{label}
			{required && (
				<Text style={{ color: Colors.Red }}>
					{" *"}
				</Text>
			)}
		</Text>
	);

	const descriptionNode = description && (
		<Text
			fz={FontSize.sm}
			c={Colors.TextDimmed}
			{...descriptionProps}
		>
			{description}
		</Text>
	);

	return (
		<Box gap="xs">
			{(label || description) && (
				<Box gap={0}>
					{labelNode}
					{descriptionNode}
				</Box>
			)}

			{children}

			{error && (
				<Text
					fz={FontSize.sm}
					c={Colors.Red}
					{...errorProps}
				>
					{error}
				</Text>
			)}
		</Box>
	);
};
