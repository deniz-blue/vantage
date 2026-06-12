import { View } from "react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Button } from "../base/Button";
import { Colors } from "../../theme/colors";
import type { OpenEvnt } from "@evnt/types";

type Status = NonNullable<OpenEvnt["status"]>;

const STATUSES: { value: Status; label: string }[] = [
	{ value: "planned", label: "Upcoming" },
	{ value: "uncertain", label: "Tentative" },
	{ value: "cancelled", label: "Cancelled" },
];

export interface StatusPickerProps {
	value: Status;
	onChange: (value: Status) => void;
}

export const StatusPicker = ({ value, onChange }: StatusPickerProps) => (
	<Box gap={8}>
		<Text fz={13} c={Colors.TextDimmed}>
			Status
		</Text>
		<View style={{ flexDirection: "row", gap: 8 }}>
			{STATUSES.map((s) => (
				<Button
					key={s.value}
					variant={value === s.value ? "filled" : "subtle"}
					color={value === s.value ? Colors.Primary : undefined}
					size="sm"
					fullWidth
					onPress={() => onChange(s.value)}
				>
					{s.label}
				</Button>
			))}
		</View>
	</Box>
);
