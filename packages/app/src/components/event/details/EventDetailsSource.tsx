import { Linking, TouchableOpacity } from "react-native";
import { useResolvedEvent } from "@vantage/core";
import type { SourceComponent } from "@evnt/types";
import { IconExternalLink, IconQuestionMark, IconDatabase, IconWorld, IconBraces, IconAt, IconBrandWikipedia } from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Sizing } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { SmallTitle } from "./SmallTitle";

export const EventDetailsSource = () => {
	const { source, format, data } = useResolvedEvent();

	const sourceComponents = data?.components?.filter(
		(c): c is SourceComponent => c.$type === "directory.evnt.component.source",
	) ?? [];

	return (
		<Box gap={Spacing.xs}>
			<SmallTitle>Source</SmallTitle>

			{sourceComponents.map((comp, i) => (
				<TouchableOpacity key={i} onPress={() => Linking.openURL(comp.url)}>
					<Box direction="row" gap={6} align="center" py={4}>
						<IconExternalLink size={16} />
						<Text fz={Sizing.fontSizeMd}>Source {sourceComponents.length > 1 ? i + 1 : ""}</Text>
					</Box>
				</TouchableOpacity>
			))}

			<SourceRow type={source.type} />

			<SmallTitle>Format</SmallTitle>

			<FormatRow type={format.type} />
		</Box>
	);
};

const sourceIcons: Record<string, React.ReactNode> = {
	unknown: <IconQuestionMark size={20} />,
	local: <IconDatabase size={20} />,
	at: <IconAt size={20} />,
	http: <IconWorld size={20} />,
	mediawiki: <IconBrandWikipedia size={20} />,
};

const sourceLabels: Record<string, string> = {
	unknown: "Unknown",
	local: "Browser/Device",
	at: "Atmosphere (AT Protocol)",
	http: "Internet (HTTP)",
	mediawiki: "MediaWiki",
};

const SourceRow = ({ type }: { type: string }) => (
	<Box direction="row" gap={8} align="center" py={2}>
		{sourceIcons[type] ?? <IconQuestionMark size={20} />}
		<Text fz={Sizing.fontSizeMd}>{sourceLabels[type] ?? type}</Text>
	</Box>
);

const formatIcons: Record<string, React.ReactNode> = {
	unknown: <IconQuestionMark size={20} />,
	"directory.evnt.event": <IconBraces size={20} />,
	ics: <IconBraces size={20} />,
	"community.lexicon.calendar.event": <IconBraces size={20} />,
};

const formatLabels: Record<string, string> = {
	unknown: "Unknown",
	"directory.evnt.event": "Open Evnt",
	ics: "iCalendar (ICS)",
	"community.lexicon.calendar.event": "Community Lexicon",
};

const FormatRow = ({ type }: { type: string }) => (
	<Box direction="row" gap={8} align="center" py={2}>
		{formatIcons[type] ?? <IconQuestionMark size={20} />}
		<Text fz={Sizing.fontSizeMd}>{formatLabels[type] ?? type}</Text>
	</Box>
);
