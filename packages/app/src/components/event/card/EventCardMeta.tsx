import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Badge } from "../../base/Badge";
import {
	sourceBadge,
	sourceBadgeFallback,
	formatBadge,
	formatBadgeFallback,
	statusBadgeColor,
} from "./event-card-config";

export const EventCardMeta = () => {
	const { source, format, data } = useResolvedEvent();

	const src = sourceBadge[source.type as keyof typeof sourceBadge] ?? sourceBadgeFallback;
	const fmt = formatBadge[format.type as keyof typeof formatBadge] ?? formatBadgeFallback;
	const status = data?.status;

	return (
		<Box direction="row" gap={4} align="center">
			<Badge variant="dot" size="sm" color={src.color}>
				{src.label}
			</Badge>
			<Badge variant="dot" size="sm" color={fmt.color}>
				{fmt.label}
			</Badge>

			{status && status !== "planned" && (
				<Badge variant="outline" size="sm" color={statusBadgeColor(status)}>
					{status}
				</Badge>
			)}
		</Box>
	);
};
