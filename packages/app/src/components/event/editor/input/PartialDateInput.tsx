import { PartialDate } from "@evnt/types";
import { Box } from "../../../base/Box";
import { Button } from "../../../base/Button";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { useState } from "react";
import { Sheet } from "../../../base/Sheet";
import { Text } from "../../../base/Text";
import { PartialDateUtil } from "@evnt/partial-date";
import { NumberInput } from "../../../base/input/NumberInput";
import { TimezoneSelect } from "../../../core/timezone-select";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { IconArrowLeft, IconArrowRight, IconCalendar, IconCheck, IconPlus, IconX } from "@tabler/icons-react-native";
import { FontSize, IconSize, Radius } from "../../../../theme/sizing";
import { CalendarMonth, CalendarYear } from "../../../core/calendar-month";
import { ActionIcon } from "../../../base/ActionIcon";
import { Colors } from "../../../../theme/colors";
import { SegmentedTextInput } from "../../../base/input/SegmentedTextInput";

export interface PartialDateInputProps {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
	label?: string;
}

export const PartialDateInput = ({
	value,
	onChange,
	label,
}: PartialDateInputProps) => {
	const [open, setOpen] = useState(false);

	const hasTime = value ? PartialDateUtil.parse(value).precision === "time" : false;
	const datePartial = value ? !PartialDateUtil.has(value, "day") : true;

	const fields = PartialDateUtil.parse(value ?? "2000[UTC]") as PartialDate.Parsed.Fields;

	const handleTimeChange = (field: "hour" | "minute", value: string) => {

	};

	const handleDateChange = (field: "year" | "month" | "day", value: string) => {

	};

	return (
		<Box>
			<InputWrapper label={label} description={value ?? "undef"} error={JSON.stringify(value ? PartialDateUtil.parse(value) : null)}>
				<Button onPress={() => {
					if (!value) onChange(PartialDateUtil.format({
						precision: "year",
						year: new Date().getFullYear(),
						timezone: useLocaleStore.getState().timezone,
					}));
					setOpen(true);
				}}>
					Set Date
				</Button>
			</InputWrapper>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box gap="md" p="md">
					<TimezoneSelect
						variant="form"
						label="Timezone"
						value={value ? PartialDateUtil.parse(value).timezone : ""}
						onChange={timezone => onChange(PartialDateUtil.format({
							...(value ? PartialDateUtil.parse(value) : {
								precision: "year",
								year: new Date().getFullYear(),
							}),
							timezone,
						}))}
					/>

					<Box>
						<Box direction="row" justify="space-between" align="center">
							<InputWrapper label="Date" />
							<Box direction="row" gap="sm">
								<Button
									py={0}
									variant="subtle"
									rightSection={(
										<Box
											w={IconSize.xs}
											h={IconSize.xs}
											justify="center"
											align="center"
											style={{
												borderColor: hasTime ? Colors.Primary : Colors.Dark1,
												backgroundColor: hasTime ? Colors.Primary : undefined,
												borderWidth: 2,
												borderRadius: Radius.xs,
											}}
										>
											{hasTime && <IconCheck size={IconSize.xs} color={Colors.White} strokeWidth={3} />}
										</Box>
									)}
								>
									<InputWrapper label="Time" />
								</Button>
							</Box>
						</Box>

						<Box direction="row" flex={1} gap="md" justify="space-between" align="center">
							<Box direction="row" gap="sm">
								<ActionIcon>
									<IconCalendar />
								</ActionIcon>

								<SegmentedTextInput
									segments={[
										{
											value: value ? fields.year.toString() : "",
											onChangeText: text => handleDateChange("year", text),
											style: { width: 44 },
											placeholder: "----",
										},
										{
											value: value && PartialDateUtil.has(value, "month") ? fields.month.toString().padStart(2, "0") : "",
											onChangeText: text => handleDateChange("month", text),
										},
										{
											value: value && PartialDateUtil.has(value, "day") ? fields.day.toString().padStart(2, "0") : "",
											onChangeText: text => handleDateChange("day", text),
										}
									]}
									common={{
										style: { width: 22 },
										selectTextOnFocus: true,
										placeholder: "--",
										keyboardType: "decimal-pad",
									}}
									separator={<Text fz={FontSize.md}>/</Text>}
								/>
							</Box>

							<SegmentedTextInput
								segments={[
									{
										value: hasTime ? fields.hour.toString().padStart(2, "0") : "",
										onChangeText: text => handleTimeChange("hour", text),
									},
									{
										value: hasTime ? fields.minute.toString().padStart(2, "0") : "",
										onChangeText: text => handleTimeChange("minute", text),
									}
								]}
								common={{
									style: { width: 22, textAlign: "center" },
									selectTextOnFocus: true,
									placeholder: "--",
									keyboardType: "decimal-pad",
								}}
								separator={<Text fz={FontSize.md}>:</Text>}
							/>
						</Box>

						<Button
							py="xs"
							px={0}
							variant="subtle"
							leftSection={(
								<Box
									w={IconSize.xs}
									h={IconSize.xs}
									justify="center"
									align="center"
									style={{
										borderColor: datePartial ? Colors.Primary : Colors.Dark1,
										backgroundColor: datePartial ? Colors.Primary : undefined,
										borderWidth: 2,
										borderRadius: Radius.xs,
									}}
								>
									{datePartial && <IconCheck size={IconSize.xs} color={Colors.White} strokeWidth={3} />}
								</Box>
							)}
						>
							<InputWrapper label="Partial Date" />
						</Button>
					</Box>

					<Box direction="row" justify="space-between" gap="sm">
						<Button
							onPress={() => {
								onChange(undefined);
								setOpen(false);
							}}
							leftSection={<IconX size={IconSize.sm} />}
						>
							Unset
						</Button>

						<Button
							variant="primary"
							onPress={() => setOpen(false)}
							leftSection={<IconCheck size={IconSize.sm} />}
						>
							Done
						</Button>
					</Box>
				</Box>
			</Sheet>
		</Box >
	);
};
