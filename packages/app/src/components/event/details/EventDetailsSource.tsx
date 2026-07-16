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
	IconClock,
} from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Colors } from "../../../theme/colors";
import { FontSize, IconSize } from "../../../theme/sizing";
import { SmallTitle } from "./SmallTitle";
import { Button } from "../../base/button/Button";

export const EventDetailsSource = () => {
	const { source, format, data, updatedAt } = useResolvedEvent();

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
					leftSection={<IconExternalLink size={IconSize.xs} color={Colors.Text} />}
				>
					<Text fz={FontSize.sm} numberOfLines={1}>
						Source Link {sourceComponents.length > 1 ? i + 1 : ""}
					</Text>
				</Button>
			))}

			<Row icon={sourceIcons[source.type]} label={sourceLabels[source.type] ?? source.type} />
			<Row icon={formatIcons[format.type]} label={formatLabels[format.type] ?? format.type} />

			<Box direction="row" align="center" gap="xs" px="sm">
				<IconClock size={IconSize.xs} color={Colors.Text} />
				<Text fz={FontSize.sm}>Last Updated:</Text>
				<Text c={Colors.TextDimmed} fz={FontSize.sm}>
					{updatedAt ? updatedAt.toLocaleString() : "Unknown"}
				</Text>
			</Box>
		</Box>
	);
};

const sourceIcons: Record<string, React.ReactNode> = {
	unknown: <IconQuestionMark size={IconSize.xs} color={Colors.Text} />,
	local: <IconDatabase size={IconSize.xs} color={Colors.Text} />,
	at: <IconAt size={IconSize.xs} color={Colors.Text} />,
	http: <IconWorld size={IconSize.xs} color={Colors.Text} />,
	mediawiki: <IconBrandWikipedia size={IconSize.xs} color={Colors.Text} />,
};

const sourceLabels: Record<string, string> = {
	unknown: "Unknown Source",
	local: "Browser/Device",
	at: "Atmosphere (AT Protocol)",
	http: "Internet (HTTP)",
	mediawiki: "MediaWiki",
};

const formatIcons: Record<string, React.ReactNode> = {
	unknown: <IconQuestionMark size={IconSize.xs} color={Colors.Text} />,
	"directory.evnt.event": <IconBraces size={IconSize.xs} color={Colors.Text} />,
	ics: <IconBraces size={IconSize.xs} color={Colors.Text} />,
	"community.lexicon.calendar.event": <IconBraces size={IconSize.xs} color={Colors.Text} />,
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
			leftSection={icon ?? <IconQuestionMark size={IconSize.xs} color={Colors.Text} />}
			variant="subtle"
			justify="flex-start"
			mih={null}
			py="xs"
		>
			{label}
		</Button>
	);
};
