import { Linking } from "react-native";
import { useResolvedEvent } from "@vantage/core";
import type { SourceComponent } from "@evnt/types";
import {
	IconExternalLink,
	IconQuestionMark,
	IconDatabase,
	IconWorld,
	IconBraces,
	IconAt,
	IconBrandWikipedia,
} from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { FontSize, IconSize } from "../../../theme/sizing";
import { SmallTitle } from "./SmallTitle";
import { Button } from "../../base/button/Button";

export const EventDetailsSource = () => {
	const { source, format, data } = useResolvedEvent();

	const sourceComponents =
		data?.components?.filter(
			(c): c is SourceComponent => c.$type === "directory.evnt.component.source",
		) ?? [];

	return (
		<Box gap="xs">
			<SmallTitle>Data</SmallTitle>

			{sourceComponents.map((comp, i) => (
				<Button
					key={i}
					onPress={() => Linking.openURL(comp.url)}
					leftSection={<IconExternalLink size={IconSize.xs} />}
				>
					<Text fz={FontSize.sm} numberOfLines={1}>
						Source Link {sourceComponents.length > 1 ? i + 1 : ""}
					</Text>
				</Button>
			))}

			<Row icon={sourceIcons[source.type]} label={sourceLabels[source.type] ?? source.type} />
			<Row icon={formatIcons[format.type]} label={formatLabels[format.type] ?? format.type} />
		</Box>
	);
};

const sourceIcons: Record<string, React.ReactNode> = {
	unknown: <IconQuestionMark size={IconSize.xs} />,
	local: <IconDatabase size={IconSize.xs} />,
	at: <IconAt size={IconSize.xs} />,
	http: <IconWorld size={IconSize.xs} />,
	mediawiki: <IconBrandWikipedia size={IconSize.xs} />,
};

const sourceLabels: Record<string, string> = {
	unknown: "Unknown Source",
	local: "Browser/Device",
	at: "Atmosphere (AT Protocol)",
	http: "Internet (HTTP)",
	mediawiki: "MediaWiki",
};

const formatIcons: Record<string, React.ReactNode> = {
	unknown: <IconQuestionMark size={IconSize.xs} />,
	"directory.evnt.event": <IconBraces size={IconSize.xs} />,
	ics: <IconBraces size={IconSize.xs} />,
	"community.lexicon.calendar.event": <IconBraces size={IconSize.xs} />,
};

const formatLabels: Record<string, string> = {
	unknown: "Unknown Format",
	"directory.evnt.event": "Open Evnt",
	ics: "iCalendar (ICS)",
	"community.lexicon.calendar.event": "Community Lexicon",
};

const Row = ({ icon, label }: { icon: React.ReactNode; label: string }) => {
	return (
		<Button
			leftSection={icon ?? <IconQuestionMark size={IconSize.xs} />}
			variant="subtle"
			justify="flex-start"
			mih={null}
			py="xs"
		>
			{label}
		</Button>
	);
};
