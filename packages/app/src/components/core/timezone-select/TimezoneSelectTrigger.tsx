import { TouchableOpacity } from "react-native";
import { IconWorld, IconChevronDown, IconArrowUp } from "@tabler/icons-react-native";
import { useComboboxCtx, ComboboxTrigger } from "../../base/combobox";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { ActionIcon } from "../../base/ActionIcon";
import { Button } from "../../base/Button";
import { Colors } from "../../../theme/colors";
import { formatOffset, getDetectedTz } from "@vantage/intl";

export interface TimezoneSelectTriggerProps {
	variant: "settings" | "form";
}

export const TimezoneSelectTrigger = ({ variant }: TimezoneSelectTriggerProps) => {
	const ctx = useComboboxCtx<string>();
	const currentOffset = formatOffset(ctx.value);
	const detectedTz = getDetectedTz();

	if (variant === "form") {
		return (
			<Button
				variant="subtle"
				size="sm"
				onPress={ctx.open}
				rightSection={<IconChevronDown size={12} color={Colors.Primary} />}
			>
				{ctx.value}
			</Button>
		);
	}

	return (
		<Box>
			<ComboboxTrigger>
				<ActionIcon bg={Colors.PrimaryLight + "33"} w={40} h={40} radius={10}>
					<IconWorld size={20} color={Colors.Primary} />
				</ActionIcon>
				<Box flex={1}>
					<Text fz={15} fw="600">
						{ctx.value}
					</Text>
					<Box mt={1}>
						<Text fz={12} c={Colors.TextDimmed}>
							{currentOffset}
						</Text>
					</Box>
				</Box>
			</ComboboxTrigger>
			{detectedTz !== ctx.value && (
				<TouchableOpacity
					onPress={() => ctx.onChange(detectedTz)}
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
