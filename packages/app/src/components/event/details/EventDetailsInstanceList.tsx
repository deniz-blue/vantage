import { useMemo, type ReactNode } from "react";
import { TouchableOpacity, Linking } from "react-native";
import { useResolvedEvent } from "@vantage/core";
import type { EventInstance, PartialDate, Venue } from "@evnt/types";
import { PartialDateUtil } from "@evnt/partial-date";
import { TranslationsUtil } from "@evnt/translations";
import { IconCalendar, IconCalendarQuestion, IconExternalLink, IconMapPin, IconWorld, IconWorldPin } from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { Sizing, FontSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";

export const EventDetailsInstanceList = () => {
	const { data } = useResolvedEvent();

	const groups = useMemo(() => {
		if (!data?.instances) return [];

		const grouped = new Map<string, EventInstance[]>();
		for (const inst of data.instances) {
			const key = JSON.stringify([...inst.venueIds].sort());
			const arr = grouped.get(key) ?? [];
			arr.push(inst);
			grouped.set(key, arr);
		}

		return Array.from(grouped.entries())
			.map(([key, instances]) => [JSON.parse(key) as string[], instances] as const)
			.map(([venueIds, instances]) => ({
				venueIds,
				instances,
				venues: venueIds
					.map((id) => data.venues?.find((v) => v.id === id))
					.filter((v): v is Venue => !!v),
			}));
	}, [data?.instances, data?.venues]);

	if (groups.length === 0) return null;

	const multipleGroups = groups.length > 1;

	return (
		<Box gap={Spacing.xs}>
			{groups.map((group, i) => {
				const showVenuesFirst = group.venueIds.length > 0 && multipleGroups;

				return (
					<Box key={i} gap={Spacing.xs} p={multipleGroups ? "xs" : undefined}>
						{showVenuesFirst
							? group.venues.map((v, j) => <MiniBoxVenue key={j} venue={v} />)
							: group.instances.map((inst, j) => <MiniBoxInstance key={j} instance={inst} />)
						}
						{showVenuesFirst
							? group.instances.map((inst, j) => <MiniBoxInstance key={j} instance={inst} />)
							: group.venues.map((v, j) => <MiniBoxVenue key={j} venue={v} />)
						}
					</Box>
				);
			})}
		</Box>
	);
};

const MiniBoxInstance = ({ instance }: { instance: EventInstance }) => {
	const language = useLocaleStore((s) => s.language);

	if (!instance.start) {
		return (
			<MiniBoxSnippet
				icon={<IconCalendarQuestion size={20} />}
				title={<Text fz={FontSize.md}>Unknown date</Text>}
			/>
		);
	}

	const hasDay = PartialDateUtil.has(instance.start, "day");
	const hasMonth = PartialDateUtil.has(instance.start, "month");

	let icon: ReactNode;
	if (hasDay) {
		const parsed = PartialDateUtil.parse(instance.start) as PartialDate.Parsed.YearMonthDay | PartialDate.Parsed.YearMonthDayTime;
		const date = new Date(Date.UTC(parsed.year, parsed.month - 1, parsed.day));
		const monthLabel = date.toLocaleDateString(language, { month: "short", timeZone: "UTC" });
		icon = (
			<Box align="center">
				<Text fz={16} fw="bold">{parsed.day}</Text>
				<Text fz={10} c="TextDimmed">{monthLabel}</Text>
			</Box>
		);
	} else if (hasMonth) {
		const parsed = PartialDateUtil.parse(instance.start) as PartialDate.Parsed.YearMonth;
		const date = new Date(Date.UTC(parsed.year, parsed.month - 1));
		const monthLabel = date.toLocaleDateString(language, { month: "short", year: "numeric", timeZone: "UTC" });
		icon = <Text fz={12}>{monthLabel}</Text>;
	} else {
		icon = <IconCalendar size={20} />;
	}

	let title: ReactNode;
	let subtitle: ReactNode | null = null;

	if (instance.end) {
		const eqPrecision = PartialDateUtil.getPrecisionEquality(instance.start, instance.end);
		const bothHasTime = PartialDateUtil.has(instance.start, "time") && PartialDateUtil.has(instance.end, "time");
		const isSameDay = eqPrecision === "day" || eqPrecision === "time";

		if (isSameDay) {
			const dayOnly = PartialDateUtil.lowerPrecision(instance.start as any, "day");
			title = <PartialDateLabel value={dayOnly} />;
			if (bothHasTime && eqPrecision !== "time") {
				const startTime = PartialDateUtil.asPlainDateTime(PartialDateUtil.parse(instance.start) as any);
				const endTime = PartialDateUtil.asPlainDateTime(PartialDateUtil.parse(instance.end) as any);
				const timeStr = `${startTime.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}–${endTime.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}`;
				subtitle = <Text fz={FontSize.sm} c="TextDimmed">{timeStr}</Text>;
			} else if (PartialDateUtil.has(instance.start, "time")) {
				const pt = PartialDateUtil.parse(instance.start) as any;
				const dt = PartialDateUtil.asPlainDateTime(pt);
				subtitle = <Text fz={FontSize.sm} c="TextDimmed">{dt.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}</Text>;
			}
		} else {
			title = <PartialDateLabel value={instance.start} />;
			subtitle = <PartialDateLabel value={instance.end} />;
		}
	} else {
		let datePart: PartialDate = instance.start;
		if (PartialDateUtil.has(instance.start, "time")) {
			datePart = PartialDateUtil.lowerPrecision(instance.start as any, "day");
		}
		title = <PartialDateLabel value={datePart} />;
		if (PartialDateUtil.has(instance.start, "time")) {
			const pt = PartialDateUtil.parse(instance.start) as any;
			const dt = PartialDateUtil.asPlainDateTime(pt);
			subtitle = <Text fz={FontSize.sm} c="TextDimmed">{dt.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}</Text>;
		}
	}

	return <MiniBoxSnippet icon={icon} title={title} subtitle={subtitle} />;
};

const PartialDateLabel = ({ value }: { value: PartialDate }) => {
	const language = useLocaleStore((s) => s.language);

	const label = useMemo(() => {
		const parsed = PartialDateUtil.parse(value);
		const currentYear = new Date().getFullYear();
		const fmt = new Intl.DateTimeFormat(language, {
			year: parsed.year !== currentYear ? "numeric" : undefined,
			month: PartialDateUtil.has(parsed, "month") ? "long" : undefined,
			day: PartialDateUtil.has(parsed, "day") ? "numeric" : undefined,
			hour: PartialDateUtil.has(parsed, "time") ? "numeric" : undefined,
			minute: PartialDateUtil.has(parsed, "time") ? "numeric" : undefined,
			calendar: "iso8601",
			hour12: false,
			timeZone: parsed.timezone,
		});
		const temporal = PartialDateUtil.asFormattableTemporal(parsed);
		return fmt.format(temporal);
	}, [value, language]);

	return <Text fz={FontSize.md}>{label}</Text>;
};

const MiniBoxVenue = ({ venue }: { venue: Venue }) => {
	const icon = venue.$type === "directory.evnt.venue.online"
		? <IconWorld size={20} />
		: venue.$type === "directory.evnt.venue.physical"
			? <IconMapPin size={20} />
			: <IconWorldPin size={20} />;

	const title = TranslationsUtil.isEmpty(venue.name)
		? <Text fz={FontSize.md} fst="italic" c="TextDimmed">Unnamed</Text>
		: <TransText fz={FontSize.md} value={venue.name} />;

	const subtitle = venue.$type === "directory.evnt.venue.physical" && venue.address
		? <AddressLabel address={venue.address} />
		: venue.$type === "directory.evnt.venue.online" && venue.url
			? <UrlLabel url={venue.url} />
			: null;

	const mapLinks = [
		venueGoogleMapsLink(venue),
		venueOpenStreetMapsLink(venue),
	].filter((l): l is string => !!l);

	return (
		<MiniBoxSnippet
			icon={icon}
			title={title}
			subtitle={
				<Box gap={4}>
					{subtitle && <Text fz={FontSize.sm} c="TextDimmed">{subtitle}</Text>}
					{mapLinks.length > 0 && (
						<Box direction="row" gap={4}>
							{mapLinks.map((link, i) => (
								<Box
									key={i}
									component={TouchableOpacity}
									bg="BackgroundLight"
									radius={4}
									px="xs"
									py={2}
									onPress={() => Linking.openURL(link)}
								>
									<Text fz={11} c="Primary">{i === 0 ? "Google Maps" : "OpenStreetMap"}</Text>
								</Box>
							))}
						</Box>
					)}
				</Box>
			}
		/>
	);
};

const AddressLabel = ({ address }: { address: { addr?: string; countryCode?: string } }) => {
	const str = [address.addr, address.countryCode].filter(Boolean).join(", ");
	return <>{str}</>;
};

const UrlLabel = ({ url }: { url: string }) => (
	<Box
		component={TouchableOpacity}
		direction="row"
		gap={4}
		align="center"
		onPress={() => Linking.openURL(url)}
	>
		<Text fz={FontSize.sm} c="Primary" numberOfLines={1}>{url}</Text>
		<IconExternalLink size={14} color="Primary" />
	</Box>
);

const venueGoogleMapsLink = (venue: Venue): string | null => {
	if (venue.$type !== "directory.evnt.venue.physical") return null;
	if (venue.address?.addr) return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(venue.address.addr)}`;
	return null;
};

const venueOpenStreetMapsLink = (venue: Venue): string | null => {
	if (venue.$type !== "directory.evnt.venue.physical") return null;
	if (venue.address?.addr) return `https://www.openstreetmap.org/search?query=${encodeURIComponent(venue.address.addr)}`;
	return null;
};

const MiniBoxSnippet = ({
	icon,
	title,
	subtitle,
}: {
	icon?: ReactNode;
	title?: ReactNode;
	subtitle?: ReactNode;
}) => {
	return (
		<Box direction="row" gap={Spacing.xs} align="flex-start">
			<Box
				w={48}
				h={48}
				align="center"
				justify="center"
				bg="BackgroundLight"
				radius={Spacing.Radius}
			>
				{icon}
			</Box>
			<Box flex={1} gap={2} pt={4}>
				{title}
				{subtitle}
			</Box>
		</Box>
	);
};
