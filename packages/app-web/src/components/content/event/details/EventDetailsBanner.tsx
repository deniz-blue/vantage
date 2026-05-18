import { ActionIcon, Box, Collapse, Group, Loader, Menu, Modal, Paper, Stack, Text, Title } from "@mantine/core";
import { useEventDetailsContext } from "./event-details-context";
import { Trans } from "../../Trans";
import { EnvelopeErrorBadge } from "../envelope/EnvelopeErrorBadge";
import type { SplashMediaComponent } from "@evnt/schema";
import { OverLayer } from "../../../base/layout/OverLayer";
import classes from "../card/event-card.module.css";
import { EvntMedia } from "../../../base/media/EvntMedia";
import { IconDotsVertical, ReactNode } from "@tabler/icons-react";
import { useActionsStore } from "../../../app/overlay/spotlight/useActionsStore";
import { useShallow } from "zustand/shallow";
import { EventTimeframeBadge } from "../badges/EventTimeframeBadge";
import { EventStatusBadge } from "../badges/EventStatusBadge";
import { TranslationsUtil } from "@evnt/translations";
import { useResolvedEvent } from "@vantage/core";

export const EventDetailsBanner = () => {
	const { data } = useResolvedEvent();
	const { loading, withModalCloseButton } = useEventDetailsContext();
	const actions = useActionsStore(
		useShallow(state => Object.values(state.actions).filter(a => a.category === "Event"))
	);

	let title: ReactNode = null;
	if (!!data && !TranslationsUtil.isEmpty(data.name))
		title = <Trans t={data.name} />;
	else if (!!data)
		title = <Text inherit inline span c="dimmed" fs="italic" children="<no title>" />;

	const splashMediaComponents = data?.components
		?.filter((c): c is SplashMediaComponent =>
			c.$type === "directory.evnt.component.splashMedia") ?? [];

	const bannerMedia = splashMediaComponents.find(x => x.roles.includes("banner"))?.media
		?? splashMediaComponents.find(x => x.roles.includes("background"))?.media;

	return (
		<Stack
			pos="sticky"
			style={{ top: !bannerMedia ? "var(--app-shell-header-height)" : 0, zIndex: 1 }}
		>
			<Paper pos="relative" style={{ overflow: "hidden" }} shadow="xs" radius={0}>
				<OverLayer>
					{bannerMedia && (
						<OverLayer>
							<EvntMedia media={bannerMedia} objectFit="cover" />
						</OverLayer>
					)}
					<OverLayer
						className={classes.dim}
						style={{ "--dim": !bannerMedia ? 0 : undefined }}
					/>
				</OverLayer>
				<Stack
					pos="relative"
					mt={bannerMedia ? "var(--app-shell-header-height, 0px)" : 0}
					p="xs"
				>
					<Group gap={4} justify="space-between" align="center">
						<Group gap={4} wrap="nowrap" align="center">
							<Collapse expanded={!!loading} orientation="horizontal">
								<Loader size="sm" />
							</Collapse>
							<Title order={3}>
								{title}
								<EventTimeframeBadge mx={4} />
								<EventStatusBadge mx={4} />
								<EnvelopeErrorBadge mx={4} />
							</Title>
						</Group>
						<Group gap={4} wrap="nowrap" align="center">
							<Menu disabled={actions.length === 0}>
								<Menu.Target>
									<ActionIcon
										size="input-md"
										color="gray"
										variant="subtle"
									>
										<IconDotsVertical />
									</ActionIcon>
								</Menu.Target>
								<Menu.Dropdown>
									{actions.map((action, i) => (
										<Menu.Item
											key={i}
											leftSection={action.icon}
											onClick={action.execute}
										>
											{action.label}
										</Menu.Item>
									))}
								</Menu.Dropdown>
							</Menu>
							{withModalCloseButton && <Modal.CloseButton />}
						</Group>
					</Group>
				</Stack>
			</Paper>
		</Stack >
	);
};
