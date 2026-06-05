import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { ResolvedEventContext } from "@vantage/core";
import { EventCard } from "../../components/content/event-card/EventCard";

export default function IndexPage() {
	return (
		<Box>
			<Text>
				<ResolvedEventContext
					value={{
						data: {
							v: "0.1",
							name: {
								en: "Test Event :3",
							},
							instances: [
								{
									venueIds: [],
									start: "2024-01-01[Europe/Istanbul]",
								}
							],
						},
						error: null,
						format: { type: "unknown" },
						id: null,
						raw: "",
						revision: {},
						source: { type: "unknown" },
					}}
				>
					<EventCard />
				</ResolvedEventContext>
			</Text>
		</Box>
	)
}
