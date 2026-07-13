import { PartialDate } from "@evnt/types";
import { Box } from "../../../base/Box";
import { Button } from "../../../base/button/Button";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { useState } from "react";
import { Sheet } from "../../../base/Sheet";
import { Text } from "../../../base/Text";
import { PartialDateUtil } from "@evnt/partial-date";
import { TimezoneSelect } from "../../../core/timezone-select";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { IconCheck, IconChevronRight, IconReload } from "@tabler/icons-react-native";
import { FontSize, IconSize } from "../../../../theme/sizing";
import { ActionIcon } from "../../../base/button/ActionIcon";
import { Colors } from "../../../../theme/colors";
import { SegmentedTextInput } from "../../../base/input/SegmentedTextInput";
import { formatDate } from "@evnt/pretty";
import { PartialDateLabel } from "../../../core/PartialDateLabel";
import { usePartialDateEditor } from "./usePartialDateEditor";

export interface PartialDateInputProps {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
	label?: string;
}

export const PartialDateInput = ({ value, onChange, label }: PartialDateInputProps) => {
	const [open, setOpen] = useState(false);
	const language = useLocaleStore((s) => s.language);
	const userTimezone = useLocaleStore((s) => s.timezone);

	const {
		paddedYear,
		paddedMonth,
		paddedDay,
		paddedHour,
		paddedMinute,
		timezone,
		disabledMonth,
		disabledDay,
		disabledTime,
		handleDateChange,
		handleTimeChange,
		setTimezone,
	} = usePartialDateEditor(value, onChange);

	const colonNormal = <Text fz={FontSize.md}>:</Text>;
	const colonDimmed = (
		<Text fz={FontSize.md} c={Colors.TextDimmed}>
			:
		</Text>
	);
	const slashNormal = <Text fz={FontSize.md}>/</Text>;
	const slashDimmed = (
		<Text fz={FontSize.md} c={Colors.TextDimmed}>
			/
		</Text>
	);

	return (
		<Box>
			<InputWrapper label={label}>
				<Button
					onPress={() => setOpen(true)}
					rightSection={<IconChevronRight size={IconSize.xs} color={Colors.TextDimmed} />}
				>
					<Text fz={FontSize.sm} c={value ? undefined : Colors.TextDimmed}>
						{value
							? formatDate(value, { language, timezone: userTimezone, compactDates: false })
							: "Unknown"}
					</Text>
				</Button>
			</InputWrapper>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box gap="md">
					<Box gap="xs">
						<Box direction="row" justify="space-between" align="center">
							<InputWrapper label="Date" />
							<Box direction="row" gap="sm">
								{/* Time toggle removed; time fields are always shown and controlled by disabledTime */}
							</Box>
						</Box>

						<Box direction="row" flex={1} gap="md" justify="space-between" align="center">
							<Box direction="row" gap="sm">
								<SegmentedTextInput
									segments={[
										{
											value: paddedYear,
											onChangeText: (text) => handleDateChange("year", text),
											style: { width: 44 },
											placeholder: "----",
											renderAfter: value
												? PartialDateUtil.has(value, "month")
													? slashNormal
													: slashDimmed
												: slashDimmed,
										},
										{
											value: paddedMonth,
											onChangeText: (text) => handleDateChange("month", text),
											focusable: !disabledMonth,
											readOnly: disabledMonth,
											renderAfter: value
												? PartialDateUtil.has(value, "day")
													? slashNormal
													: slashDimmed
												: slashDimmed,
										},
										{
											value: paddedDay,
											onChangeText: (text) => handleDateChange("day", text),
											focusable: !disabledDay,
											readOnly: disabledDay,
										},
									]}
									common={{
										style: { width: 22 },
										selectTextOnFocus: true,
										placeholder: "--",
										keyboardType: "decimal-pad",
									}}
								/>
							</Box>

							<SegmentedTextInput
								segments={[
									{
										value: paddedHour,
										onChangeText: (text) => handleTimeChange("hour", text),
										renderAfter: !disabledTime ? colonNormal : colonDimmed,
									},
									{
										value: paddedMinute,
										onChangeText: (text) => handleTimeChange("minute", text),
									},
								]}
								common={{
									style: { width: 22, textAlign: "center" },
									selectTextOnFocus: true,
									placeholder: "--",
									keyboardType: "decimal-pad",
									readOnly: disabledTime,
									focusable: !disabledTime,
								}}
							/>
						</Box>
					</Box>

					<Box gap="xs">
						<Box direction="row" justify="space-between">
							<InputWrapper label="Timezone" />
							<InputWrapper label="Reset" />
						</Box>
						<Box direction="row" gap="sm">
							<Box flex={1}>
								<TimezoneSelect
									variant="form"
									value={value ? PartialDateUtil.parse(value).timezone : ""}
									onChange={(timezone) => setTimezone(timezone)}
								/>
							</Box>
							<ActionIcon
								disabled={!value || timezone === userTimezone}
								onPress={() => setTimezone(userTimezone)}
							>
								<IconReload color={Colors.Text} />
							</ActionIcon>
						</Box>
					</Box>

					<Box>
						<InputWrapper label="Preview" />
						<Box direction="row" gap="sm">
							{value ? (
								<PartialDateLabel value={value} />
							) : (
								<Text c="TextDimmed">Unknown date</Text>
							)}
						</Box>
					</Box>

					<Box direction="row" justify="flex-end" gap="sm">
						<Button
							variant="primary"
							onPress={() => setOpen(false)}
							leftSection={<IconCheck size={IconSize.sm} color={Colors.Text} />}
						>
							Done
						</Button>
					</Box>
				</Box>
			</Sheet>
		</Box>
	);
};
