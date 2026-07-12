import { Venue } from "@evnt/types";
import { Button } from "../../base/button/Button";
import { IconExternalLink } from "@tabler/icons-react-native";
import { IconSize } from "../../../theme/sizing";
import { Fragment, useState } from "react";
import { Sheet } from "../../base/Sheet";
import { Box } from "../../base/Box";
import { Linking } from "react-native";

export const OpenMapButton = ({ venue }: { venue?: Venue }) => {
	const [open, setOpen] = useState(false);

	if (venue?.$type !== "directory.evnt.venue.physical") return null;
	if (!venue.address?.addr) return null;

	return (
		<Fragment>
			<Box align="flex-start">
				<Button
					size="sm"
					rightSection={<IconExternalLink size={IconSize.xs} />}
					onPress={() => setOpen(true)}
				>
					Open in Maps
				</Button>
			</Box>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<MapsSheetContent addr={venue.address.addr} />
			</Sheet>
		</Fragment>
	);
};

export const MapsSheetContent = ({ addr }: { addr: string }) => {
	const apps = [
		{
			url: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(addr)}`,
			name: "Google Maps",
		},
		{
			url: `https://maps.apple.com/?q=${encodeURIComponent(addr)}`,
			name: "Apple Maps",
		},
		{
			url: `https://www.openstreetmap.org/search?query=${encodeURIComponent(addr)}`,
			name: "OpenStreetMap",
		},
	] as const;

	return (
		<Box gap="sm">
			{apps.map((app) => (
				<Button key={app.name} onPress={() => Linking.openURL(app.url)}>
					{app.name}
				</Button>
			))}
		</Box>
	);
};
