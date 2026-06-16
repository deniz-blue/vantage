import { type ReactNode } from "react";
import { Text, type TextProps } from "./Text";
import { Colors } from "../../theme/colors";
import { FontSize } from "../../theme/sizing";
import { Box } from "./Box";

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
	return (
		<Box gap="xs">
			<Box gap={0}>
				{label && (
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
				)}

				{description && (
					<Text
						fz={FontSize.sm}
						c={Colors.TextDimmed}
						{...descriptionProps}
					>
						{description}
					</Text>
				)}
			</Box>

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
