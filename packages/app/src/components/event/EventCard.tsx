import { useResolvedEvent } from "@vantage/core";
import { TranslationsUtil } from "@evnt/translations";
import { TouchableOpacity, View } from "react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Colors } from "../../theme/colors";

// === Badge Colors by source type ===

const sourceColor: Record<string, string> = {
	local: Colors.TextDimmed,
	http: "#4CAF50",
	at: "#4C84FF",
	mediawiki: "#9C27B0",
};

const sourceLabel: Record<string, string> = {
	local: "L",
	http: "H",
	at: "@",
	mediawiki: "W",
};

const formatLabel: Record<string, string> = {
	ics: "ICS",
	"community.lexicon.calendar.event": "C",
	unknown: "?",
};

const formatColor: Record<string, string> = {
	ics: "#FF9800",
	"community.lexicon.calendar.event": "#00BCD4",
	unknown: "#FFC107",
};

// === EventCard ===

export interface EventCardProps {
	onPress?: () => void;
}

export const EventCard = ({ onPress }: EventCardProps) => {
	const resolved = useResolvedEvent();
	const error = resolved.error;
	const data = resolved.data;
	const sourceType = resolved.source.type;
	const formatType = resolved.format.type;

	const name = data && !TranslationsUtil.isEmpty(data.name)
		? TranslationsUtil.translate(data.name)
		: null;

	const label = data?.label && !TranslationsUtil.isEmpty(data.label)
		? TranslationsUtil.translate(data.label)
		: null;

	return (
		<TouchableOpacity onPress={onPress} disabled={!onPress}>
			<Box
				style={{
					backgroundColor: Colors.BackgroundLight,
					borderRadius: 8,
					padding: 12,
					marginHorizontal: 8,
					marginVertical: 4,
					borderWidth: error ? 1 : 0,
					borderColor: error ? "#f44336" : undefined,
				}}
			>
				{/* Header row: name + badges */}
				<View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
					{/* Name */}
					<View style={{ flex: 1 }}>
						<Text
							style={{
								fontWeight: "bold",
								fontSize: 16,
							}}
							numberOfLines={1}
						>
							{name ?? "<no title>"}
						</Text>
						{label && (
							<Text
								style={{
									fontSize: 13,
									color: Colors.TextDimmed,
								}}
								numberOfLines={1}
							>
								{label}
							</Text>
						)}
					</View>

					{/* Badges */}
					<View style={{ flexDirection: "row", gap: 4 }}>
						<Badge text={sourceLabel[sourceType] ?? "?"} color={sourceColor[sourceType] ?? "#888"} />
						<Badge text={formatLabel[formatType] ?? "?"} color={formatColor[formatType] ?? "#888"} />
					</View>
				</View>

				{/* Error indicator */}
				{error && (
					<Text style={{ color: "#f44336", fontSize: 12, marginTop: 4 }}>
						{error.kind ?? "error"}: {error.message}
					</Text>
				)}
			</Box>
		</TouchableOpacity>
	);
};

// === Badge helper ===

const Badge = ({ text, color }: { text: string; color: string }) => (
	<View
		style={{
			backgroundColor: color + "33",
			borderRadius: 4,
			paddingHorizontal: 6,
			paddingVertical: 2,
			borderWidth: 1,
			borderColor: color,
		}}
	>
		<Text style={{ color, fontSize: 11, fontWeight: "bold" }}>
			{text}
		</Text>
	</View>
);
