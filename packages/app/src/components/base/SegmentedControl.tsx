import { ReactNode } from "react";
import { InputWrapper, InputWrapperProps } from "./input/InputWrapper";
import { Box } from "./Box";
import { Text } from "./Text";
import { Button } from "./Button";
import { FontSize } from "../../theme/sizing";

export interface SegmentedControlProps<T extends string> extends Omit<InputWrapperProps, "children"> {
	value: T;
	onChange: (value: T) => void;
	options: { label: ReactNode; value: T }[];
};

export const SegmentedControl = <T extends string>({
	value,
	onChange,
	options,
	...rest
}: SegmentedControlProps<T>) => {
	return (
		<InputWrapper {...rest}>
			<Box direction="row" flex={1} gap="xs">
				{options.map((option) => (
					<Button
						key={option.value}
						flex={1}
						align="center"
						py={0}
						px={0}
					>
						{typeof option.label === "string" ? (
							<Text fz={FontSize.sm}>{option.label}</Text>
						) : option.label}
					</Button>
				))}
			</Box>
		</InputWrapper>
	)
};
