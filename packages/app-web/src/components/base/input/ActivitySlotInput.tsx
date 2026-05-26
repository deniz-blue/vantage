import { ActivitySlot, Time } from "@evnt/schema";
import { Group, Input, NumberInput, Stack, TextInput } from "@mantine/core";
import { TimePicker } from "@mantine/dates";

export const ActivitySlotInput = ({
	value,
	onChange,
	label,
}: {
	value: ActivitySlot | undefined;
	onChange: (value: ActivitySlot | undefined) => void;
	label?: string;
}) => {
	return (
		<Stack gap={4}>
			<Group>
				<NumberInput
					label="Day"
					value={value?.day ?? 0}
					onChange={(v) => onChange({ ...(value || {}), day: (Number(v) > 0) ? Number(v) : undefined })}
					maw={60}
				/>

				<TimePicker
					label="Time"
					value={value?.time}
					onChange={(v) => onChange({ ...(value || {}), time: v as Time })}
				/>

				<TextInput
					label="Duration"
					disabled
				/>
			</Group>
		</Stack>
	)
};