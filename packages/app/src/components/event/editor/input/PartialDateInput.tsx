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
	const PARTS = ["year", "month", "day", "time"] as const;
	const [part, setPart] = useState<null | "year" | "month" | "day" | "time">(null);

	const currentPrecision = value ? PartialDateUtil.getPrecision(value) : "none";

	const nextPart = PARTS[PARTS.indexOf(part!) + 1];
	const prevPart = PARTS[PARTS.indexOf(part!) - 1];
	const canGoNext = !!nextPart && value && PartialDateUtil.has(value, nextPart as any);
	const canAddNext = part === currentPrecision && currentPrecision !== "time";

	const segmentButton = (sgt: (typeof PARTS)[number]) => {
		if (!value) return null;

		const hasSegment: boolean = PartialDateUtil.has(value, sgt as any);
		const segmentValue = hasSegment ? (PartialDateUtil.parse(value) as any)[sgt] as number | undefined : undefined;
		const prevSegment = PARTS[PARTS.indexOf(sgt) - 1] as "year" | "month" | "day" | null;
		const hasPrevSegment = prevSegment ? PartialDateUtil.has(value, prevSegment as any) : false;

		if (!hasSegment && hasPrevSegment) return (
			<Button
				onPress={() => {
					onChange(PartialDateUtil.setPrecision(value, sgt as any, "low"));
					setPart(sgt);
				}}
			>
				+
			</Button>
		);

		if (!hasSegment) return null;

		return (
			<Button
				onPress={() => {
					setPart(sgt);
				}}
			>
				{segmentValue ?? sgt.toUpperCase()}
			</Button>
		);
	};

	return (
		<Box>
			<InputWrapper label={label} description={value ?? "undef"} error={JSON.stringify(value ? PartialDateUtil.parse(value) : null)}>
				{currentPrecision === "none" ? (
					<Button onPress={() => {
						setPart("year");
						onChange(PartialDateUtil.format({
							precision: "year",
							year: Temporal.Now.plainDateISO().year,
							timezone: useLocaleStore.getState().timezone,
						}));
					}}>
						Set Date
					</Button>
				) : (
					<Box direction="row">
						{segmentButton("year")}
						{segmentButton("month")}
						{segmentButton("day")}
						{segmentButton("time")}
					</Box>
				)}
			</InputWrapper>

			<Sheet open={!!part} onClose={() => setPart(null)}>
				<Box gap="md" p="md" justify="space-between" flex={1}>
					<Box gap="md" py="md">
						{part === "year" && value && (
							<Box>
								<TimezoneSelect
									variant="form"
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

						{part === "month" && value && (
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

						{part === "day" && value && (
							<Box>
								<CalendarMonth
									year={PartialDateUtil.parse(value).year}
									month={(PartialDateUtil.parse(value) as PartialDate.Parsed.YearMonth).month!}
									renderDay={({ day, isOutsideMonth, isToday }) => (
										<ActionIcon style={{ aspectRatio: 1 }}>
											<Text
												fz={FontSize.sm}
												c={isOutsideMonth ? "TextDimmed" : isToday ? "Primary" : undefined}
												children={day}
											/>
										</ActionIcon>
									)}
								/>
							</Box>
						)}
					</Box>
					<Box direction="row" justify="space-between" gap="md">
						<Box direction="row" gap="sm">
							{!!prevPart && (
								<Button
									onPress={() => setPart(prevPart)}
									leftSection={<IconArrowLeft size={IconSize.sm} />}
								>
									{prevPart[0].toUpperCase() + prevPart.slice(1)}
								</Button>
							)}

							<Button
								onPress={() => {
									if (part === "year") {
										onChange(undefined);
									} else {
										const prevPrecision = PARTS[PARTS.indexOf(part!) - 1];
										onChange(PartialDateUtil.lowerPrecision(value!, prevPrecision as any));
									}
									setPart(null);
								}}
								leftSection={<IconX size={IconSize.sm} />}
							>
								<Text fz={FontSize.sm}>
									Unset {part ? (part[0].toUpperCase() + part.slice(1)) : ""}
								</Text>
							</Button>
						</Box>

						<Box direction="row" gap="sm">
							<Button
								onPress={() => setPart(null)}
								leftSection={<IconCheck size={IconSize.sm} />}
							>
								Keep
							</Button>

							{!!nextPart && canGoNext && (
								<Button
									onPress={() => setPart(nextPart)}
									rightSection={<IconArrowRight size={IconSize.sm} />}
								>
									{nextPart[0].toUpperCase() + nextPart.slice(1)}
								</Button>
							)}

							{canAddNext && (
								<Button
									onPress={() => {
										onChange(PartialDateUtil.setPrecision(value!, nextPart as any, "low"));
										setPart(nextPart);
									}}
									leftSection={<IconPlus size={IconSize.sm} />}
									variant="filled"
								>
									<Text fz={FontSize.sm} c="White">
										{nextPart[0].toUpperCase() + nextPart.slice(1)}
									</Text>
								</Button>
							)}
						</Box>
					</Box>
				</Box>
			</Sheet>
		</Box>
	);
};
