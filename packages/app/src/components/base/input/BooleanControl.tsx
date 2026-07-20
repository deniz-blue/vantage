import { InputWrapperProps } from "./InputWrapper";
import { Box } from "../Box";
import { IconCheck, IconSlash, IconX } from "@tabler/icons-react-native";
import { Text } from "../Text";
import { FontSize, IconSize, Radius } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";
import { ActionIcon } from "../button/ActionIcon";

export interface BooleanControlProps extends Omit<InputWrapperProps, "children"> {
	value: boolean | null;
	onChange: (value: boolean | null) => void;
}

export const BooleanControl = ({ value, onChange, label, description }: BooleanControlProps) => {
	return (
		<Box direction="row" gap="sm" align="center" justify="space-between">
			<Box>
				<Text>{label}</Text>
				<Text c="TextDimmed" fz={FontSize.sm}>
					{description}
				</Text>
			</Box>
			<Box direction="row" gap="xs" p={4} bg={Colors.BackgroundLight} radius={Radius.Default}>
				{(
					[
						{ value: false, icon: IconX, color: Colors.Red },
						{ value: null, icon: IconSlash, color: undefined },
						{ value: true, icon: IconCheck, color: Colors.Green },
					] as const
				).map(({ icon: Icon, value: v, color }) => (
					<ActionIcon
						key={JSON.stringify(v)}
						size="md"
						color={value === v ? color : undefined}
						selected={value === v}
						onPress={() => onChange(v)}
					>
						<Icon size={IconSize.md} color={Colors.Text} />
					</ActionIcon>
				))}
			</Box>
		</Box>
	);
};
