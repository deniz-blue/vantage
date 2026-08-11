import type { EventStatus } from "@evnt/types";
import { ComponentType, memo } from "react";
import { Box } from "../../../base/Box";
import {
	IconCalendarCheck,
	IconCalendarOff,
	IconCalendarPause,
	IconCalendarQuestion,
	IconCalendarTime,
	IconProps,
} from "@tabler/icons-react-native";
import { Text } from "../../../base/Text";
import { Colors } from "../../../../theme/colors";
import { FontSize } from "../../../../theme/sizing";
import { Select } from "../../../base/input/Select";

const ICONS: Record<EventStatus, ComponentType<IconProps>> = {
	planned: IconCalendarCheck,
	cancelled: IconCalendarOff,
	postponed: IconCalendarTime,
	suspended: IconCalendarPause,
	uncertain: IconCalendarQuestion,
};

const iconProps: IconProps = {
	size: 18,
	color: Colors.Text,
};

const DATA = ["planned", "uncertain", "postponed", "suspended", "cancelled"] as EventStatus[];

export const StatusPicker = memo(
	({ value, onChange }: { value: EventStatus; onChange: (status: EventStatus) => void }) => {
		return (
			<Select
				label="Status"
				data={DATA}
				value={value ?? "planned"}
				onChange={onChange}
				renderItem={StatusPickerItem}
				searchable={false}
			/>
		);
	},
);

export const StatusPickerItem = memo(({ value }: { value: EventStatus }) => {
	const Icon = ICONS[value];

	return (
		<Box gap="sm" direction="row" align="center" flex={1}>
			<Icon {...iconProps} />
			<Text fz={FontSize.sm}>{value[0]?.toUpperCase() + value.slice(1)}</Text>
		</Box>
	);
});
