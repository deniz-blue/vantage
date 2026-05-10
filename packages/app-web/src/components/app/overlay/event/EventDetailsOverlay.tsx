import { useEventDetailsModal } from "../../../../hooks/app/useEventDetailsModal";
import { BaseOverlay } from "../base/BaseOverlay";
import { EventDetailsContent } from "../../../content/event/details/EventDetailsContent";
import { useEventQuery } from "../../../../db/useEventQuery";
import { Affix, Button, Space, Transition } from "@mantine/core";
import { useProvideEventActions } from "../../../../hooks/actions/useProvideEventActions";
import { Link } from "@tanstack/react-router";
import { IconArrowNarrowRight } from "@tabler/icons-react";
import { ResolvedEventContext } from "../../../../db/resolved-event";

export const EventDetailsOverlay = () => {
	const { close, useValue } = useEventDetailsModal();
	const id = useValue();

	return (
		<BaseOverlay
			opened={!!id}
			onClose={close}
			modalBodyProps={{ p: 0 }}
		>
			{id && (
				<EventDetailsOverlayHandler id={id} />
			)}

			<Space h="10rem" />

			<Transition mounted={!!id}>
				{(styles) => (
					<Affix
						position={{ bottom: "sm", right: "50%" }}
						pos="fixed"
						style={styles}
					>
						{!!id && (
							<Button
								variant="filled"
								color="gray"
								miw="10rem"
								rightSection={<IconArrowNarrowRight />}
								style={{ transform: "translateX(50%)" }}
								renderRoot={(props) => (
									<Link
										to="/event"
										search={{ id }}
										{...props}
									/>
								)}
							>
								View
							</Button>
						)}
					</Affix>
				)}
			</Transition>
		</BaseOverlay>
	);
};

export const EventDetailsOverlayHandler = ({ id }: { id: Vantage.EventId }) => {
	const query = useEventQuery(id);

	useProvideEventActions(query.data);

	return (
		<ResolvedEventContext value={query.data ?? null}>
			<EventDetailsContent
				loading={query.isFetching}
				withModalCloseButton
			/>
		</ResolvedEventContext>
	);
};
