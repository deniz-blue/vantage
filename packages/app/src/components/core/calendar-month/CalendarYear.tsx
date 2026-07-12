import { useMemo } from "react";
import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { IconCheck } from "@tabler/icons-react-native";
import { FontSize, IconSize } from "../../../theme/sizing";
import { Text } from "../../base/Text";

export interface CalendarYearProps {
	selectedMonth?: number;
	onSelectMonth?: (month: number) => void;
}

export const CalendarYear = ({ selectedMonth, onSelectMonth }: CalendarYearProps) => {
	const locale = useLocaleStore((s) => s.language);
	const currentMonth = Temporal.Now.plainDateISO().month;

	const monthNames = useMemo(() => {
		const fmt = new Intl.DateTimeFormat(locale, { month: "long" });
		return Array.from({ length: 12 }, (_, i) => {
			const date = new Temporal.PlainDate(2024, i + 1, 1);
			return fmt.format(date);
		});
	}, [locale]);

	return (
		<Box gap="sm" flex={1}>
			{[0, 3, 6, 9].map((rowStart) => (
				<Box direction="row" gap="sm" key={rowStart} flex={1}>
					{monthNames.slice(rowStart, rowStart + 3).map((name, i) => {
						const month = rowStart + i + 1;
						const isSelected = month === selectedMonth;

						return (
							<Box flex={1} key={month}>
								<Button
									onPress={onSelectMonth ? () => onSelectMonth(month) : undefined}
									selected={isSelected}
									rightSection={isSelected && <IconCheck size={IconSize.xs} />}
									leftSection={
										<Text
											c={isSelected ? "Text" : month === currentMonth ? "Primary" : "TextDimmed"}
											fz={FontSize.xs}
										>
											{month.toString()}
										</Text>
									}
								>
									{name}
								</Button>
							</Box>
						);
					})}
				</Box>
			))}
		</Box>
	);
};
