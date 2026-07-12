import { Fragment, useMemo, useState, type ReactNode } from "react";
import { Linking } from "react-native";
import { useResolvedEvent } from "@vantage/core";
import type { EventInstance, PartialDate, PhysicalVenue, Venue } from "@evnt/types";
import { PartialDateUtil } from "@evnt/partial-date";
import {
	IconCalendar,
	IconCalendarQuestion,
	IconExternalLink,
	IconMapPin,
	IconWorld,
	IconWorldPin,
} from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { Text, TextProps } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { FontSize, IconSize, Radius } from "../../../theme/sizing";
import { MapsSheetContent } from "../venue/OpenMapButton";
import { Sheet } from "../../base/Sheet";
import { InputWrapper } from "../../base/input/InputWrapper";
import { AppCopyButton } from "../../core/AppCopyButton";
import { Colors } from "../../../theme/colors";
import { PartialDateLabel } from "../../core/PartialDateLabel";

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
		<Box gap="xs">
			{groups.map((group, i) => {
				const showVenuesFirst = group.venueIds.length > 0 && multipleGroups;

				return (
					<Box key={i} gap="xs" p={multipleGroups ? "xs" : undefined}>
						{showVenuesFirst
							? group.venues.map((v, j) => <MiniBoxVenue key={j} venue={v} />)
							: group.instances.map((inst, j) => <MiniBoxInstance key={j} instance={inst} />)}
						{showVenuesFirst
							? group.instances.map((inst, j) => <MiniBoxInstance key={j} instance={inst} />)
							: group.venues.map((v, j) => <MiniBoxVenue key={j} venue={v} />)}
					</Box>
				);
			})}
		</Box>
	);
};

const MiniBoxInstance = ({ instance }: { instance: EventInstance }) => {
	const [open, setOpen] = useState(false);
	const language = useLocaleStore((s) => s.language);

	if (!instance.start) {
		return (
			<MiniBoxSnippet
				icon={<IconCalendarQuestion size={IconSize.sm} />}
				title={<Text fz={FontSize.md}>Unknown date</Text>}
			/>
		);
	}

	const hasDay = PartialDateUtil.has(instance.start, "day");
	const hasMonth = PartialDateUtil.has(instance.start, "month");

	let icon: ReactNode;
	if (hasDay) {
		const parsed = PartialDateUtil.parse(instance.start) as
			| PartialDate.Parsed.YearMonthDay
			| PartialDate.Parsed.YearMonthDayTime;
		const date = new Date(Date.UTC(parsed.year, parsed.month - 1, parsed.day));
		const monthLabel = date.toLocaleDateString(language, { month: "short", timeZone: "UTC" });
		icon = (
			<Box align="center">
				<Text fz={FontSize.md} fw="bold">
					{parsed.day}
				</Text>
				<Text fz={FontSize.xs} c="TextDimmed" numberOfLines={1}>
					{monthLabel}
				</Text>
			</Box>
		);
	} else if (hasMonth) {
		// const parsed = PartialDateUtil.parse(instance.start) as PartialDate.Parsed.YearMonth;
		// const date = new Date(Date.UTC(parsed.year, parsed.month - 1));
		// const monthLabel = date.toLocaleDateString(language, { month: "short", timeZone: "UTC" });
		// icon = <Text fz={FontSize.xs} numberOfLines={1}>{monthLabel}</Text>;
		icon = <IconCalendar size={IconSize.sm} />;
	} else {
		icon = <IconCalendar size={IconSize.sm} />;
	}

	let title: ReactNode;
	let subtitle: ReactNode | null = null;

	if (instance.end) {
		const eqPrecision = PartialDateUtil.getPrecisionEquality(instance.start, instance.end);
		const bothHasTime =
			PartialDateUtil.has(instance.start, "time") && PartialDateUtil.has(instance.end, "time");
		const isSameDay = eqPrecision === "day" || eqPrecision === "time";

		if (isSameDay) {
			const dayOnly = PartialDateUtil.lowerPrecision(instance.start as any, "day");
			title = <PartialDateLabel value={dayOnly} fz={FontSize.md} />;
			if (bothHasTime && eqPrecision !== "time") {
				const startTime = PartialDateUtil.asPlainDateTime(
					PartialDateUtil.parse(instance.start) as any,
				);
				const endTime = PartialDateUtil.asPlainDateTime(PartialDateUtil.parse(instance.end) as any);
				const timeStr = `${startTime.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}–${endTime.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}`;
				subtitle = (
					<Text fz={FontSize.sm} c="TextDimmed">
						{timeStr}
					</Text>
				);
			} else if (PartialDateUtil.has(instance.start, "time")) {
				const pt = PartialDateUtil.parse(instance.start) as any;
				const dt = PartialDateUtil.asPlainDateTime(pt);
				subtitle = (
					<Text fz={FontSize.sm} c="TextDimmed">
						{dt.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}
					</Text>
				);
			}
		} else {
			title = <PartialDateLabel value={instance.start} fz={FontSize.md} />;
			subtitle = <PartialDateLabel value={instance.end} fz={FontSize.md} />;
		}
	} else {
		let datePart: PartialDate = instance.start;
		if (PartialDateUtil.has(instance.start, "time")) {
			datePart = PartialDateUtil.lowerPrecision(instance.start as any, "day");
		}
		title = <PartialDateLabel value={datePart} fz={FontSize.md} />;
		if (PartialDateUtil.has(instance.start, "time")) {
			const pt = PartialDateUtil.parse(instance.start) as any;
			const dt = PartialDateUtil.asPlainDateTime(pt);
			subtitle = (
				<Text fz={FontSize.sm} c="TextDimmed" numberOfLines={1}>
					{dt.toLocaleString(language, { hour: "2-digit", minute: "2-digit", hour12: false })}
				</Text>
			);
		}
	}

	return (
		<Fragment>
			<MiniBoxSnippet icon={icon} title={title} subtitle={subtitle} onPress={() => setOpen(true)} />

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box gap="sm">
					{instance.start && (
						<Fragment>
							<InputWrapper label="Starts at" />
							<PartialDateLabel value={instance.start} />
						</Fragment>
					)}
					{instance.end && (
						<Fragment>
							<InputWrapper label="Ends at" />
							<PartialDateLabel value={instance.end} />
						</Fragment>
					)}
				</Box>
			</Sheet>
		</Fragment>
	);
};

const MiniBoxVenue = ({ venue }: { venue: Venue }) => {
	const [open, setOpen] = useState(false);

	const icon =
		venue.$type === "directory.evnt.venue.online" ? (
			<IconWorld size={IconSize.sm} />
		) : venue.$type === "directory.evnt.venue.physical" ? (
			<IconMapPin size={IconSize.sm} />
		) : (
			<IconWorldPin size={IconSize.sm} />
		);

	const title = (
		<TransText fz={FontSize.md} value={venue.name} numberOfLines={1} fallback="Unnamed" />
	);

	const subtitle =
		venue.$type === "directory.evnt.venue.physical" && venue.address ? (
			<AddressLabel numberOfLines={1} address={venue.address} fz={FontSize.sm} c="TextDimmed" />
		) : venue.$type === "directory.evnt.venue.online" && venue.url ? (
			<Text numberOfLines={1} c="Blue" children={venue.url} />
		) : null;

	const address = venue.$type === "directory.evnt.venue.physical" ? venue.address : undefined;

	const link = venue.$type === "directory.evnt.venue.online" ? venue.url : undefined;

	return (
		<Fragment>
			<MiniBoxSnippet icon={icon} title={title} onPress={() => setOpen(true)} subtitle={subtitle} />

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box p="sm" gap="sm">
					<InputWrapper label="Location" />

					<TransText fz={FontSize.md} value={venue.name} fallback="Unnamed" />

					{address && (
						<Box gap="sm">
							<InputWrapper label="Address" />
							<AddressLabel address={address} fz={FontSize.md} c="Text" />
							{address.addr && (
								<Fragment>
									<AppCopyButton value={address.addr}>Copy Address</AppCopyButton>
									<InputWrapper label="Open in Maps" />
									<MapsSheetContent addr={address.addr} />
								</Fragment>
							)}
						</Box>
					)}

					{link && (
						<Fragment>
							<InputWrapper label="Link" />
							<Button
								onPress={() => Linking.openURL(link)}
								leftSection={<IconExternalLink size={IconSize.xs} />}
							>
								<TransText fz={FontSize.md} value={venue.name} fallback="Unnamed" />
							</Button>
						</Fragment>
					)}
				</Box>
			</Sheet>
		</Fragment>
	);
};

const AddressLabel = ({
	address,
	...props
}: TextProps & {
	address: PhysicalVenue.Address;
}) => {
	const str = [address.addr, address.countryCode].filter(Boolean).join(", ");
	return <Text {...props}>{str}</Text>;
};

const MiniBoxSnippet = ({
	icon,
	title,
	subtitle,
	onPress,
}: {
	icon?: ReactNode;
	title?: ReactNode;
	subtitle?: ReactNode;
	onPress?: () => void;
}) => {
	return (
		<Button
			onPress={onPress}
			bg="transparent"
			flex={1}
			px="xs"
			py="xs"
			style={{
				borderColor: Colors.Dark5,
				borderWidth: 1,
			}}
		>
			<Box direction="row" gap="xs" align="flex-start" flex={1}>
				<Box
					w={48}
					h={48}
					align="center"
					justify="center"
					bg="BackgroundLight"
					radius={Radius.Default}
				>
					{icon}
				</Box>
				<Box flex={1} gap="xs" pt="xs">
					{title}
					{subtitle}
				</Box>
			</Box>
		</Button>
	);
};
