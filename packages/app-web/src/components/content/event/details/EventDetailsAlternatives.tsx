import { parseResourceUri } from "@atcute/lexicons";
import { Stack, Text } from "@mantine/core";
import { EventLinkButtonBase } from "../link/EventLinkButtonBase";
import { IconExternalLink } from "@tabler/icons-react";
import { SmallTitle } from "../../base/SmallTitle";
import { useResolvedEvent } from "@vantage/core";

export const EventDetailsAlternatives = () => {
	const { source } = useResolvedEvent();

	const aturi = source.type == "at" ? parseResourceUri(source.uri) : null;

	const atlinks = [];

	if (aturi?.collection === "community.lexicon.calendar.event") {
		atlinks.push({
			text: "atmo.rsvp",
			url: `https://atmo.rsvp/p/${aturi.repo}/e/${aturi.rkey}`,
			icon: <img src="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>%F0%9F%97%93%EF%B8%8F</text></svg>" alt="atmo.rsvp" width={24} height={24} />,
		});
		atlinks.push({
			text: "OpenMeet",
			url: `https://platform.openmeet.net/at/${aturi.repo}/${aturi.collection}/${aturi.rkey}`,
			icon: <img src="https://platform.openmeet.net/openmeet/icons/favicon.ico" alt="OpenMeet" width={24} height={24} />,
		});
		atlinks.push({
			text: "Smoke Signal",
			url: `https://smokesignal.events/${aturi.repo}/${aturi.rkey}`,
			icon: <img src="https://smokesignal.events/favicon.ico" alt="Smoke Signal" width={24} height={24} style={{ borderRadius: "50%" }} />,
		});
	};

	if (!atlinks.length) return null;

	return (
		<Stack gap={4}>
			<SmallTitle>
				alternatives
			</SmallTitle>
			<Stack gap={4}>
				{atlinks.map(({ text, url, icon }) => (
					<EventLinkButtonBase
						key={url}
						url={url}
						leftSection={icon}
					>
						<Text inherit span mr={4}>
							{text}
						</Text> <IconExternalLink size={14} />
					</EventLinkButtonBase>
				))}
			</Stack>
		</Stack>
	)
};
