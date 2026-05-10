import { SimpleGrid, Stack } from "@mantine/core";
import type { EventQuery } from "../../../db/useEventQuery";
import { EventCard } from "../event/card/EventCard";
import { EventContextMenu } from "../event/EventContextMenu";
import { ResolvedEventContext } from "../../../db/resolved-event";

export const EventsGrid = ({
	queries,
}: {
	queries: EventQuery[];
}) => {
	let gap = 4;
	let itemWidth = 300;

	let p = (n: number) => n * itemWidth + (n - 1) * gap;

	return (
		<Stack>
			<SimpleGrid
				type="container"
				cols={{
					base: 1,
					[p(2) + "px"]: 2,
					[p(3) + "px"]: 3,
					[p(4) + "px"]: 4,
					[p(5) + "px"]: 5,
					[p(6) + "px"]: 6,
					[p(7) + "px"]: 7,
					[p(8) + "px"]: 8,
					[p(9) + "px"]: 9,
					[p(10) + "px"]: 10,
				}}
				verticalSpacing={gap}
				spacing={gap}
				data-grow
				data-hover
			>
				{queries.map((query, index) => (
					<ResolvedEventContext key={index} value={query.data ?? null}>
						<EventCard
							variant="card"
							loading={query.isFetching}
							menu={<EventContextMenu />}
						/>
					</ResolvedEventContext>
				))}
			</SimpleGrid>
		</Stack>
	);
};
