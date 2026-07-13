import type { EventStatus } from "@evnt/types";
import { ComponentType } from "react";
import { Box } from "../../../base/Box";
import {
	Combobox,
	ComboboxSheetList,
	ComboboxSheet,
	ComboboxTrigger,
} from "../../../base/combobox";
import {
	IconCalendarCheck,
	IconCalendarOff,
	IconCalendarPause,
	IconCalendarQuestion,
	IconCalendarTime,
	IconProps,
} from "@tabler/icons-react-native";
import { Text } from "../../../base/Text";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { Colors } from "../../../../theme/colors";
import { FontSize } from "../../../../theme/sizing";

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

export const StatusPicker = ({
	value,
	onChange,
}: {
	value: EventStatus;
	onChange: (status: EventStatus) => void;
}) => {
	const Icon = ICONS[value];

	return (
		<Combobox value={value ?? "planned"} onChange={onChange}>
			<InputWrapper label="Status">
				<ComboboxTrigger>
					<Box direction="row" align="center" gap="sm">
						<Icon {...iconProps} />
						<Text fz={FontSize.sm}>{value[0]?.toUpperCase() + value.slice(1)}</Text>
					</Box>
				</ComboboxTrigger>
			</InputWrapper>
			<ComboboxSheet>
				<ComboboxSheetList
					data={["planned", "uncertain", "postponed", "suspended", "cancelled"] as EventStatus[]}
					renderItem={(status) => {
						const Icon = ICONS[status];

						return (
							<Box gap="sm" direction="row" align="center" flex={1}>
								<Icon {...iconProps} />
								<Text fz={FontSize.sm}>{status[0]?.toUpperCase() + status.slice(1)}</Text>
							</Box>
						);
					}}
				/>
			</ComboboxSheet>
		</Combobox>
	);
};
