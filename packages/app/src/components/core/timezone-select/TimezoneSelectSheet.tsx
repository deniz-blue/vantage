import { useRef, useState } from "react";
import { LayoutChangeEvent, ScrollView } from "react-native";
import { useComboboxCtx } from "../../base/combobox";
import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { Divider } from "../../base/Divider";
import { Text } from "../../base/Text";
import { FontSize } from "../../../theme/sizing";
import { formatOffset } from "@vantage/intl";
import { SheetScrollView } from "../../base/Sheet";

const RegionRow = ({
	region,
	selected,
	onPress,
	onLayout,
}: {
	region: string;
	selected: boolean;
	onPress: () => void;
	onLayout?: (e: LayoutChangeEvent) => void;
}) => (
	<Button
		onPress={onPress}
		selected={selected}
		w="100%"
		onLayout={selected ? onLayout : undefined}
		px={12}
		py={10}
		justify="flex-start"
	>
		{region}
	</Button>
);

const TimezoneRow = ({
	tz,
	selected,
	onPress,
	onLayout,
	withRegion,
}: {
	tz: string;
	selected: boolean;
	onPress: () => void;
	onLayout?: (e: LayoutChangeEvent) => void;
	withRegion?: boolean;
}) => {
	const offset = formatOffset(tz);
	const sub = withRegion ? tz : tz.replace(/^[^/]+\//, "").replace(/_/g, " ");
	return (
		<Button
			onPress={onPress}
			onLayout={selected ? onLayout : undefined}
			selected={selected}
			w="100%"
			px={12}
			py={10}
			justify="flex-start"
		>
			<Box direction="row" align="center" gap={10}>
				<Box flex={1}>
					<Text fz={FontSize.md - 1} fw={selected ? "600" : "400"} numberOfLines={1}>
						{sub}
					</Text>
				</Box>
				<Text fz={FontSize.xs} fw="700" c={selected ? "Text" : "TextDimmed"}>
					{offset || "—"}
				</Text>
			</Box>
		</Button>
	);
};

const allTz = Intl.supportedValuesOf("timeZone");

const allRegions: readonly string[] = Array.from(
	new Set(
		allTz.filter((tz) => tz.includes("/") && !tz.startsWith("Etc/")).map((tz) => tz.split("/")[0]!),
	),
).sort((a, b) => a.localeCompare(b));

const timezonesInRegion = (region: string): readonly string[] =>
	allTz.filter((tz) => tz.startsWith(region + "/"));

export const TimezoneSelectSheet = () => {
	const ctx = useComboboxCtx<string>();
	const [region, setRegion] = useState<string | null>(null);

	const activeRegion = region ?? (ctx.value.includes("/") ? ctx.value.split("/")[0]! : "Other");
	const tzRegionList = timezonesInRegion(activeRegion);

	const regionScrollRef = useRef<ScrollView>(null);
	const tzScrollRef = useRef<ScrollView>(null);

	const onRegionLayout = (e: LayoutChangeEvent) => {
		regionScrollRef.current?.scrollTo({ y: e.nativeEvent.layout.y, animated: false });
	};

	const onTzLayout = (e: LayoutChangeEvent) => {
		tzScrollRef.current?.scrollTo({ y: e.nativeEvent.layout.y, animated: false });
	};

	const onTzSelect = (tz: string) => {
		ctx.onChange(tz);
		ctx.close();
	};

	if (ctx.search) {
		const tzList = allTz.filter((tz) => {
			if (!ctx.search.trim()) return true;
			const q = ctx.search.toLowerCase();
			return tz.toLowerCase().includes(q) || formatOffset(tz).toLowerCase().includes(q);
		});

		return (
			<SheetScrollView>
				<Box gap="xs" p="sm">
					{tzList.length === 0 ? (
						<Box align="center" justify="center" py="xl">
							<Text fz={FontSize.sm} c="TextDimmed">
								No matching timezones
							</Text>
						</Box>
					) : (
						tzList.map((item) => (
							<TimezoneRow
								key={item}
								tz={item}
								selected={item === ctx.value}
								onPress={() => onTzSelect(item)}
								onLayout={onTzLayout}
								withRegion
							/>
						))
					)}
				</Box>
			</SheetScrollView>
		);
	}

	return (
		<Box direction="row" flex={1}>
			<Box flex={1}>
				<SheetScrollView ref={regionScrollRef}>
					<Box gap="xs" p="sm">
						<TimezoneRow
							tz="UTC"
							selected={ctx.value === "UTC"}
							onPress={() => onTzSelect("UTC")}
							onLayout={ctx.value === "UTC" ? onTzLayout : undefined}
						/>
						{allRegions.map((item) => (
							<RegionRow
								key={item}
								region={item}
								selected={item === activeRegion}
								onPress={() => setRegion(item)}
								onLayout={onRegionLayout}
							/>
						))}
					</Box>
				</SheetScrollView>
			</Box>

			<Divider vertical mx={0} thickness={1} />

			<Box flex={1}>
				<SheetScrollView ref={tzScrollRef} key={activeRegion}>
					<Box gap="xs" p="sm">
						{tzRegionList.map((item) => (
							<TimezoneRow
								key={item}
								tz={item}
								selected={item === ctx.value}
								onPress={() => onTzSelect(item)}
								onLayout={onTzLayout}
							/>
						))}
					</Box>
				</SheetScrollView>
			</Box>
		</Box>
	);
};
