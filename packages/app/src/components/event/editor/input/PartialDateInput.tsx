import { PartialDate } from "@evnt/types";
import { Box } from "../../../base/Box";
import { Button } from "../../../base/Button";
import { InputWrapper } from "../../../base/InputWrapper";
import { useState } from "react";
import { Sheet } from "../../../base/Sheet";
import { Text } from "../../../base/Text";
import { PartialDateUtil } from "@evnt/partial-date";
import { NumberInput } from "../../../base/NumberInput";
import { TimezoneSelect } from "../../../core/timezone-select";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { IconArrowLeft, IconArrowRight, IconCheck, IconPlus, IconX } from "@tabler/icons-react-native";
import { FontSize, IconSize } from "../../../../theme/sizing";
import { CalendarMonth, CalendarYear } from "../../../core/calendar-month";
import { ActionIcon } from "../../../base/ActionIcon";
import { Colors } from "../../../../theme/colors";

export interface PartialDateInputProps {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
	label?: string;
}

type PrecisionLevel = "year" | "month" | "day" | "time" | "none";
const PRECISION_LEVELS = ["year", "month", "day", "time"] as const;

export const PartialDateInput = ({
	value,
	onChange,
	label,
}: PartialDateInputProps) => {
	const [open, setOpen] = useState(false);

	const currentPrecision: PrecisionLevel = value ? PartialDateUtil.getPrecision(value) : "none";
	const nextPrecision: PrecisionLevel | undefined = currentPrecision === "none" ? "year" : PRECISION_LEVELS[PRECISION_LEVELS.indexOf(currentPrecision) + 1];

	const canAddNext = currentPrecision !== "time";

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
				<Box gap="md" p="md" justify="space-between" flex={1}>
					<Box gap="md" py="md">
						{currentPrecision === "year" && value && (
							<Box gap="md">
								<TimezoneSelect
									variant="form"
									label="Timezone"
									value={PartialDateUtil.parse(value).timezone}
									onChange={timezone => onChange(PartialDateUtil.format({
										...PartialDateUtil.parse(value),
										timezone,
									}))}
								/>

								<NumberInput
									label="Year"
									value={PartialDateUtil.parse(value).year}
									onChange={year => onChange(PartialDateUtil.format({
										...PartialDateUtil.parse(value),
										year: year ?? 0,
									}))}
								/>
							</Box>
						)}

						{currentPrecision === "month" && value && (
							<Box flex={1}>
								<CalendarYear
									selectedMonth={(PartialDateUtil.parse(value) as PartialDate.Parsed.YearMonth).month}
									onSelectMonth={(month) => onChange(PartialDateUtil.format({
										...PartialDateUtil.parse(value),
										month,
									} as PartialDate.Parsed))}
								/>
							</Box>
						)}

						{currentPrecision === "day" && value && (
							<Box>
								<CalendarMonth
									year={PartialDateUtil.parse(value).year}
									month={(PartialDateUtil.parse(value) as PartialDate.Parsed.YearMonth).month!}
									gap="xs"
									renderDay={({ day, isOutsideMonth, isToday, month, year }) => {
										const parsed = PartialDateUtil.parse(value) as PartialDate.Parsed.Fields;
										const isSelected = parsed.year === year && parsed.month === month && parsed.day === day;

										return (
											<ActionIcon
												style={{ aspectRatio: 1 }}
												bg={isSelected ? Colors.PrimaryLight + "22" : undefined}
												onPress={() => onChange(PartialDateUtil.format({
													...PartialDateUtil.parse(value),
													day,
													month,
													year,
												} as PartialDate.Parsed))}
											>
												<Text
													fz={FontSize.sm}
													c={isOutsideMonth ? "TextDimmed" : (isToday || isSelected) ? "Primary" : undefined}
													children={day}
												/>
											</ActionIcon>
										);
									}}
								/>
							</Box>
						)}
					</Box>
					<Box direction="row" justify="space-between" gap="md">
						<Box direction="row" gap="sm">
							<Button
								onPress={() => {
									if (currentPrecision === "year") {
										onChange(undefined);
										setOpen(false);
									} else {
										const prevPrecision = PRECISION_LEVELS[PRECISION_LEVELS.indexOf(currentPrecision as any) - 1];
										onChange(PartialDateUtil.lowerPrecision(value!, prevPrecision as any));
									}
								}}
								leftSection={<IconX size={IconSize.sm} />}
							>
								<Text fz={FontSize.sm}>
									Unset {currentPrecision ? (currentPrecision[0].toUpperCase() + currentPrecision.slice(1)) : ""}
								</Text>
							</Button>
						</Box>

						<Box direction="row" gap="sm">
							<Button
								onPress={() => setOpen(false)}
								leftSection={<IconCheck size={IconSize.sm} />}
							>
								Keep
							</Button>

							{canAddNext && (
								<Button
									onPress={() => {
										onChange(PartialDateUtil.setPrecision(value!, nextPrecision as any, "low"));
									}}
									leftSection={<IconPlus size={IconSize.sm} />}
									variant="primary"
								>
									<Text fz={FontSize.sm} c="White">
										{nextPrecision[0].toUpperCase() + nextPrecision.slice(1)}
									</Text>
								</Button>
							)}
						</Box>
					</Box>
				</Box>
			</Sheet>
		</Box >
	);
};
