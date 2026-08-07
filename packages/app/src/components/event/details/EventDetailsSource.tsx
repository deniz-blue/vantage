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
	IconProps,
	IconBroadcast,
} from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Colors } from "../../../theme/colors";
import { FontSize, IconSize } from "../../../theme/sizing";
import { SmallTitle } from "./SmallTitle";
import { Button } from "../../base/button/Button";
import { ComponentType } from "react";

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

			<Row
				icon={sourceIcons[source.type] ?? IconQuestionMark}
				label={sourceLabels[source.type] ?? source.type}
			/>
			<Row
				icon={formatIcons[format.type] ?? IconQuestionMark}
				label={formatLabels[format.type] ?? format.type}
			/>

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

const sourceIcons: Partial<Record<string, ComponentType<IconProps>>> = {
	unknown: IconQuestionMark,
	local: IconDatabase,
	at: IconAt,
	http: IconWorld,
	mediawiki: IconBrandWikipedia,
	folio: IconBroadcast,
};

const sourceLabels: Partial<Record<string, string>> = {
	unknown: "Unknown Source",
	local: "Browser/Device",
	at: "Atmosphere (AT Protocol)",
	http: "Internet (HTTP)",
	folio: "Internet (Folio)",
	mediawiki: "MediaWiki",
};

const formatIcons: Partial<Record<string, ComponentType<IconProps>>> = {
	unknown: IconQuestionMark,
	"directory.evnt.event": IconBraces,
	ics: IconBraces,
	"community.lexicon.calendar.event": IconBraces,
};

const formatLabels: Partial<Record<string, string>> = {
	unknown: "Unknown Format",
	"directory.evnt.event": "Open Evnt",
	ics: "iCalendar (ICS)",
	"community.lexicon.calendar.event": "Community Lexicon",
};

const Row = ({ icon: Icon, label }: { icon: ComponentType<IconProps>; label: string }) => {
	return (
		<Box direction="row" align="center" gap="xs" px="sm">
			<Icon size={IconSize.xs} color={Colors.Text} />
			<Text fz={FontSize.sm}>{label}</Text>
		</Box>
	);
};
