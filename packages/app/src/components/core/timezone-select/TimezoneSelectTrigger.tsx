import { IconWorld, IconArrowUp } from "@tabler/icons-react-native";
import { useComboboxCtx, ComboboxTrigger } from "../../base/combobox";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { ActionIcon } from "../../base/ActionIcon";
import { Button } from "../../base/Button";
import { Colors } from "../../../theme/colors";
import { formatOffset, getDetectedTz } from "@vantage/intl";
import { FontSize, IconSize } from "../../../theme/sizing";

export interface TimezoneSelectTriggerProps {
	variant: "settings" | "form";
}

export const TimezoneSelectTrigger = ({ variant }: TimezoneSelectTriggerProps) => {
	const ctx = useComboboxCtx<string>();
	const currentOffset = formatOffset(ctx.value);
	const detectedTz = getDetectedTz();

	if (variant === "form") {
		return (
			<ComboboxTrigger>
				<Text fz={FontSize.sm}>
					{ctx.value || "Select timezone"}
				</Text>
			</ComboboxTrigger>
		);
	}

	return (
		<Box>
			<ComboboxTrigger>
				<Box direction="row" align="center" gap="sm">
					<ActionIcon bg={Colors.PrimaryLight + "33"}>
						<IconWorld size={IconSize.md} color={Colors.Primary} />
					</ActionIcon>
					<Box flex={1} gap="xs">
						<Text fz={FontSize.sm} fw="600">
							{ctx.value}
						</Text>
						<Text fz={FontSize.xs} c={Colors.TextDimmed}>
							{currentOffset}
						</Text>
					</Box>
				</Box>
			</ComboboxTrigger>
			{detectedTz !== ctx.value && (
				<Button
					onPress={() => ctx.onChange(detectedTz)}
					mt="sm"
					align="center"
					bg={Colors.PrimaryLight + "22"}
				>
					<Box direction="row" align="center" gap="xs" flex={1}>
						<IconArrowUp size={IconSize.sm} color={Colors.Primary} />
						<Text fz={FontSize.sm} c="Primary" fw="500">
							Use detected: {detectedTz} ({formatOffset(detectedTz)})
						</Text>
					</Box>
				</Button>
			)}
		</Box>
	);
};
