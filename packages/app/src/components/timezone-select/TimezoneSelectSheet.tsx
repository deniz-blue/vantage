import { useCallback, useRef } from "react";
import { LayoutChangeEvent, ScrollView, TouchableOpacity } from "react-native";
import { Box } from "../base/Box";
import { Divider } from "../base/Divider";
import { Text } from "../base/Text";
import { Sizing } from "../../theme/sizing";
import { Colors } from "../../theme/colors";
import { formatOffset } from "@vantage/intl";

export interface TimezoneSelectSheetProps {
	value: string;
	region: string | null;
	onSelect: (tz: string) => void;
	onSelectRegion: (region: string) => void;
}

const RegionRow = ({ region, selected, onPress, onLayout }: { region: string; selected: boolean; onPress: () => void; onLayout?: (e: LayoutChangeEvent) => void }) => (
	<Box
		px="md"
		py={12}
		mx="xs"
		my={2}
		radius={Sizing.radiusSm}
		bg={selected ? Colors.Primary + "22" : undefined}
		onLayout={selected ? onLayout : undefined}
	>
		<TouchableOpacity onPress={onPress} activeOpacity={0.7}>
			<Text fz={Sizing.fontSizeMd} fw={selected ? "600" : "500"}>{region}</Text>
		</TouchableOpacity>
	</Box>
);

const TimezoneRow = ({ tz, selected, onPress, onLayout }: { tz: string; selected: boolean; onPress: () => void; onLayout?: (e: LayoutChangeEvent) => void }) => {
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
			onLayout={selected ? onLayout : undefined}
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

const allTz = Intl.supportedValuesOf("timeZone");

const allRegions: readonly string[] = Array.from(
	new Set(
		allTz.map((tz) =>
			tz.includes("/") ? tz.split("/")[0]! : "Other",
		),
	),
).sort((a, b) => a.localeCompare(b));

const timezonesInRegion = (region: string): readonly string[] =>
	region === "Other"
		? allTz.filter((tz) => !tz.includes("/"))
		: allTz.filter((tz) => tz.startsWith(region + "/"));

export const TimezoneSelectSheet = ({
	value,
	region,
	onSelect,
	onSelectRegion,
}: TimezoneSelectSheetProps) => {
	const activeRegion = region ?? (value.includes("/") ? value.split("/")[0]! : "Other");
	const tzList = timezonesInRegion(activeRegion);

	const regionScrollRef = useRef<ScrollView>(null);
	const tzScrollRef = useRef<ScrollView>(null);

	const onRegionLayout = useCallback(
		(e: LayoutChangeEvent) => {
			regionScrollRef.current?.scrollTo({ y: e.nativeEvent.layout.y, animated: false });
		},
		[],
	);

	const onTzLayout = useCallback(
		(e: LayoutChangeEvent) => {
			tzScrollRef.current?.scrollTo({ y: e.nativeEvent.layout.y, animated: false });
		},
		[],
	);

	return (
		<Box direction="row" flex={1}>
			<Box flex={1}>
				<ScrollView
					ref={regionScrollRef}
					contentContainerStyle={{ flexGrow: 1, justifyContent: "center" }}
				>
					<Box py={4} pr={4}>
						{allRegions.map((item) => (
							<RegionRow
								key={item}
								region={item}
								selected={item === activeRegion}
								onPress={() => onSelectRegion(item)}
								onLayout={onRegionLayout}
							/>
						))}
					</Box>
				</ScrollView>
			</Box>

			<Divider vertical thickness={1} color={Colors.Dark5} />

			<Box flex={1}>
				<ScrollView
					ref={tzScrollRef}
					key={activeRegion}
					contentContainerStyle={{ flexGrow: 1, justifyContent: "center" }}
				>
					<Box px={4} py={4} gap={2}>
						{tzList.map((item) => (
							<TimezoneRow
								key={item}
								tz={item}
								selected={item === value}
								onPress={() => onSelect(item)}
								onLayout={onTzLayout}
							/>
						))}
					</Box>
				</ScrollView>
			</Box>
		</Box>
	);
};
