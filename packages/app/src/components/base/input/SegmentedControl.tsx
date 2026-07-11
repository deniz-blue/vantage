import { ReactNode } from "react";
import { InputWrapper, InputWrapperProps } from "./InputWrapper";
import { Box } from "../Box";
import { Button, ButtonProps } from "../button/Button";
import { Radius } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";

export interface SegmentedControlProps<T> extends Omit<InputWrapperProps, "children"> {
	value: T;
	onChange: (value: T) => void;
	options: { label: ReactNode; value: T; color?: ButtonProps["color"] }[];
	buttonProps?: Omit<ButtonProps, "children">;
}

export const SegmentedControl = <T,>({
	value,
	onChange,
	options,
	buttonProps,
	...rest
}: SegmentedControlProps<T>) => {
	return (
		<InputWrapper {...rest}>
			<Box
				direction="row"
				flex={1}
				gap="xs"
				p={4}
				bg={Colors.BackgroundLight}
				radius={Radius.Default}
			>
				{options.map((option) => (
					<Button
						key={typeof option.value === "string" ? option.value : JSON.stringify(option.value)}
						selected={value === option.value}
						color={value === option.value ? option.color : undefined}
						flex={1}
						onPress={() => onChange(option.value)}
						{...buttonProps}
					>
						{option.label}
					</Button>
				))}
			</Box>
		</InputWrapper>
	);
};
