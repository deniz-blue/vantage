import { InputWrapperProps } from "./InputWrapper";
import { Box } from "../Box";
import { SegmentedControl } from "./SegmentedControl";
import { IconCheck, IconSlash, IconX } from "@tabler/icons-react-native";
import { Text } from "../Text";
import { FontSize, IconSize } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";

export interface BooleanControlProps extends Omit<InputWrapperProps, "children"> {
	value: boolean | null;
	onChange: (value: boolean | null) => void;
}

export const BooleanControl = ({ value, onChange, label, description }: BooleanControlProps) => {
	return (
		<Box direction="row" gap="sm" align="center" flex={1}>
			<Box flex={1}>
				<Text>{label}</Text>
				<Text c="TextDimmed" fz={FontSize.sm}>
					{description}
				</Text>
			</Box>
			<SegmentedControl
				value={value}
				onChange={onChange}
				options={[
					{ label: <IconX size={IconSize.xs} />, value: false, color: Colors.Red },
					{ label: <IconSlash size={IconSize.xs} />, value: null },
					{ label: <IconCheck size={IconSize.xs} />, value: true, color: Colors.Green },
				]}
				buttonProps={{ size: "sm" }}
			/>
		</Box>
	);
};
