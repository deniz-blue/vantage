import { TouchableOpacity } from "react-native";
import { IconWorld, IconChevronDown, IconArrowUp } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Button } from "../base/Button";
import { Colors } from "../../theme/colors";
import { formatOffset, getDetectedTz } from "./timezone-data";

export interface TimezoneSelectTriggerProps {
	value: string;
	variant: "settings" | "form";
	onOpen: () => void;
	onChange: (value: string) => void;
}

export const TimezoneSelectTrigger = ({
	value,
	variant,
	onOpen,
	onChange,
}: TimezoneSelectTriggerProps) => {
	const currentOffset = formatOffset(value);
	const detectedTz = getDetectedTz();

	if (variant === "form") {
		return (
			<Button
				variant="subtle"
				size="sm"
				onPress={onOpen}
				rightSection={<IconChevronDown size={12} color={Colors.Primary} />}
			>
				{value}
			</Button>
		);
	}

	return (
		<Box>
			<Button
				variant="default"
				onPress={onOpen}
				leftSection={<IconWorld size={20} color={Colors.Primary} />}
			>
				<Box flex={1}>
					<Text fz={15} fw="600">
						{value}
					</Text>
					<Text fz={12} c={Colors.TextDimmed} mt={1}>
						{currentOffset}
					</Text>
				</Box>
			</Button>
			{detectedTz !== value && (
				<TouchableOpacity
					onPress={() => onChange(detectedTz)}
					activeOpacity={0.7}
					style={{
						marginTop: 8,
						flexDirection: "row",
						alignItems: "center",
						backgroundColor: Colors.PrimaryLight + "22",
						paddingVertical: 8,
						paddingHorizontal: 12,
						borderRadius: 8,
						gap: 6,
					}}
				>
					<IconArrowUp size={14} color={Colors.Primary} />
					<Text fz={13} c="Primary" fw="500">
						Use detected: {detectedTz} ({formatOffset(detectedTz)})
					</Text>
				</TouchableOpacity>
			)}
		</Box>
	);
};
