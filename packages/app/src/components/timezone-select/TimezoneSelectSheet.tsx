import { FlatList, TouchableOpacity } from "react-native";
import { IconArrowLeft } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Sizing } from "../../theme/sizing";
import { Colors } from "../../theme/colors";
import { allRegions, timezonesInRegion, formatOffset } from "./timezone-data";

export interface TimezoneSelectSheetProps {
	value: string;
	region: string | null;
	onSelect: (tz: string) => void;
	onSelectRegion: (region: string) => void;
	onBack: () => void;
}

// === Row components ===

const RegionRow = ({ region, onPress }: { region: string; onPress: () => void }) => (
	<Box px="md" py={14} mx="sm" my={2} radius={Sizing.radiusMd}>
		<TouchableOpacity onPress={onPress} activeOpacity={0.7}>
			<Box direction="row" align="center">
				<Text fz={Sizing.fontSizeMd} fw="500">{region}</Text>
			</Box>
		</TouchableOpacity>
	</Box>
);

const TimezoneRow = ({ tz, selected, onPress }: { tz: string; selected: boolean; onPress: () => void }) => {
	const offset = formatOffset(tz);
	const sub = tz.replace(/^[^/]+\//, "").replace(/_/g, " ");
	return (
		<Box
			direction="row"
			align="center"
			py={10}
			px={12}
			radius={6}
			gap={10}
			bg={selected ? Colors.Primary + "22" : undefined}
		>
			<TouchableOpacity
				onPress={onPress}
				activeOpacity={0.6}
				style={{ flex: 1, flexDirection: "row", alignItems: "center", gap: 10 }}
			>
				<Box miw={52} align="flex-end">
					<Text fz={Sizing.fontSizeSm} fw="700" c={selected ? Colors.Primary : Colors.TextDimmed}>
						{offset || "—"}
					</Text>
				</Box>
				<Box flex={1}>
					<Text fz={Sizing.fontSizeMd - 1} fw={selected ? "600" : "400"} numberOfLines={1}>
						{sub}
					</Text>
				</Box>
			</TouchableOpacity>
		</Box>
	);
};

// === List header (back button) ===

const RegionHeader = ({ region, onBack }: { region: string; onBack: () => void }) => (
	<TouchableOpacity onPress={onBack} activeOpacity={0.7}>
		<Box bg={Colors.Background} px={10} py={14} direction="row" align="center" gap={6}>
			<IconArrowLeft size={18} color={Colors.Primary} />
			<Text fz={14} c="Primary" fw="500">{region}</Text>
		</Box>
	</TouchableOpacity>
);

// === Component ===

export const TimezoneSelectSheet = ({
	value,
	region,
	onSelect,
	onSelectRegion,
	onBack,
}: TimezoneSelectSheetProps) => {
	const HEADER = "__tz_header__";
	const raw = region ? timezonesInRegion(region) : allRegions;
	const data = region ? [HEADER, ...raw] : raw;

	return (
		<FlatList
			data={data}
			keyExtractor={(item) => item}
			style={{ flex: 1 }}
			keyboardShouldPersistTaps="handled"
			initialNumToRender={region ? 30 : undefined}
			stickyHeaderIndices={region ? [0] : undefined}
			contentContainerStyle={
				region
					? { paddingHorizontal: 8, gap: 2, paddingBottom: 32 }
					: { paddingVertical: 4 }
			}
			renderItem={({ item }) => {
				if (item === HEADER) {
					return <RegionHeader region={region!} onBack={onBack} />;
				}
				return region ? (
					<TimezoneRow
						tz={item}
						selected={item === value}
						onPress={() => onSelect(item)}
					/>
				) : (
					<RegionRow
						region={item}
						onPress={() => onSelectRegion(item)}
					/>
				);
			}}
		/>
	);
};
