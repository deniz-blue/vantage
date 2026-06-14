import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Badge } from "../../base/Badge";
import { TransText } from "../../core/TransText";
import { Sizing } from "../../../theme/sizing";

const statusBadgeProps: Record<string, { color: string; label: string }> = {
	cancelled: { color: "Red", label: "Cancelled" },
	postponed: { color: "Yellow", label: "Postponed" },
	suspended: { color: "Grey", label: "Suspended" },
	uncertain: { color: "Dark2", label: "Uncertain" },
};

export const EventDetailsBanner = () => {
	const { data } = useResolvedEvent();
	const status = data?.status;

	const statusBadge = status && status !== "planned"
		? statusBadgeProps[status] ?? null
		: null;

	return (
		<Box px="md" pt="md" pb="sm" gap={4}>
			<TransText
				fz={28}
				fw="bold"
				value={data?.name}
				fallback={<Text fz={16} fst="italic" c="TextDimmed">Untitled event</Text>}
			/>
			{data?.label && (
				<TransText fz={Sizing.fontSizeMd} c="TextDimmed" value={data.label} />
			)}
			{statusBadge && (
				<Box direction="row" gap={4} mt={4}>
					<Badge variant="outline" size="sm" color={statusBadge.color}>
						{statusBadge.label}
					</Badge>
				</Box>
			)}
		</Box>
	);
};
