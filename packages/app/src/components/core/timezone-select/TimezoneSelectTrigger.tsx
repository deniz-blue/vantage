import { IconWorld, IconArrowUp } from "@tabler/icons-react-native";
import { useComboboxCtx, ComboboxTrigger } from "../../base/combobox";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Button } from "../../base/button/Button";
import { Colors } from "../../../theme/colors";
import { formatOffset, getDetectedTz } from "@vantage/intl";
import { FontSize, IconSize, Radius } from "../../../theme/sizing";

export const TimezoneSelectTrigger = ({
	value,
	variant,
}: {
	value: string;
	variant: "settings" | "form";
}) => {
	const ctx = useComboboxCtx<string>();
	const currentOffset = formatOffset(value);
	const detectedTz = getDetectedTz();

	if (variant === "form") {
		return (
			<ComboboxTrigger>
				<Text fz={FontSize.sm}>{value || "Select timezone"}</Text>
			</ComboboxTrigger>
		);
	}

	return (
		<Box>
			<ComboboxTrigger py="sm" px="sm">
				<Box direction="row" align="center" gap="sm">
					<Box p="xs" radius={Radius.Default} bg={Colors.PrimaryLight + "33"}>
						<IconWorld size={IconSize.md} color={Colors.Primary} />
					</Box>
					<Box flex={1} gap="xs">
						<Text fz={FontSize.sm} fw="600">
							{value}
						</Text>
						<Text fz={FontSize.xs} c={Colors.TextDimmed}>
							{currentOffset}
						</Text>
					</Box>
				</Box>
			</ComboboxTrigger>
			{detectedTz !== value && (
				<Button
					onPress={() => ctx.onOptionSubmit(detectedTz)}
					mt="sm"
					justify="flex-start"
					leftSection={<IconArrowUp size={IconSize.sm} color={Colors.TextDimmed} />}
				>
					Use detected: {detectedTz} ({formatOffset(detectedTz)})
				</Button>
			)}
		</Box>
	);
};
