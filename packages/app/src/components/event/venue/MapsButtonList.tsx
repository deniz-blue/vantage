import { Button } from "../../base/button/Button";
import { Box } from "../../base/Box";
import { Linking } from "react-native";

export const MapsButtonList = ({ addr }: { addr: string }) => {
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
