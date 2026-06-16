import type { EventStatus } from "@evnt/types";
import { ComponentType, JSX, useState } from "react";
import { Box } from "../../../base/Box";
import { Combobox, ComboboxList, ComboboxSheet, ComboboxTrigger } from "../../../base/combobox";
import { IconCalendarCheck, IconCalendarOff, IconCalendarPause, IconCalendarQuestion, IconCalendarTime, IconProps } from "@tabler/icons-react-native";
import { Text } from "../../../base/Text";
import { InputWrapper } from "../../../base/InputWrapper";
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
};

export const StatusPicker = ({
	value,
	onChange,
}: {
	value: EventStatus;
	onChange: (status: EventStatus) => void;
}) => {
	const [open, setOpen] = useState(false);

	const Icon = ICONS[value];

	return (
		<Combobox value={value ?? "planned"} onChange={onChange}>
			<InputWrapper label="Status" description="The current status of the event">
				<ComboboxTrigger>
					<Box direction="row" align="center" gap="sm">
						<Icon {...iconProps} />
						<Text fz={FontSize.sm}>{value[0]?.toUpperCase() + value.slice(1)}</Text>
					</Box>
				</ComboboxTrigger>
			</InputWrapper>
			<ComboboxSheet>
				<ComboboxList
					data={["planned", "uncertain", "postponed", "suspended", "cancelled"] as EventStatus[]}
					renderItem={(status, selected) => {
						const Icon = ICONS[status];

						return (
							<Box gap="sm" direction="row" align="center" flex={1}>
								<Icon {...iconProps} />
								<Text fz={FontSize.sm}>
									{status[0]?.toUpperCase() + status.slice(1)}
								</Text>
							</Box>
						);
					}}
				/>
			</ComboboxSheet>
		</Combobox>
	)
};
