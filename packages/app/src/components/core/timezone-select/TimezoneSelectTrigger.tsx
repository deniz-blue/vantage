import { IconWorld, IconArrowUp } from "@tabler/icons-react-native";
import { useComboboxCtx, ComboboxTrigger } from "../../base/combobox";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { ActionIcon } from "../../base/button/ActionIcon";
import { Button } from "../../base/button/Button";
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
			<ComboboxTrigger py="sm" px="sm">
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
					justify="flex-start"
					leftSection={<IconArrowUp size={IconSize.sm} color={Colors.TextDimmed} />}
				>
					Use detected: {detectedTz} ({formatOffset(detectedTz)})
				</Button>
			)}
		</Box >
	);
};
