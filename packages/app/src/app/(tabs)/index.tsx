import { useCallback, useState } from "react";
import { useRouter } from "expo-router";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Fab } from "../../components/base/Fab";
import { Button } from "../../components/base/Button";
import { Sheet } from "../../components/base/Sheet";
import { useHomeStore } from "../../stores/useHomeStore";
import { HomeWidget } from "../../components/home/HomeWidget";
import { Container } from "../../components/base/Container";

export default function IndexPage() {
	const router = useRouter();
	const widgets = useHomeStore((s) => s.widgets);
	const [sheetOpen, setSheetOpen] = useState(false);

	const toggleSheet = useCallback(() => setSheetOpen((p) => !p), []);
	const handleTestSheetClose = useCallback(() => setSheetOpen(false), []);

	return (
		<Container size="lg" flex={1} px={0}>
			<Box px="md" pt="md" pb={4}>
				<Text fz={24} fw="bold">
					Vantage
				</Text>
				<Text fz={13} c="TextDimmed">
					An open-source event calendar for everyone
				</Text>
			</Box>

			<Fab onPress={() => router.push("/new")} />

			{widgets.map((widget, index) => (
				<HomeWidget key={index} widget={widget} />
			))}

			<Box px="md" py="md">
				<Button onPress={toggleSheet}>
					{sheetOpen ? "Close sheet" : "Open test sheet"}
				</Button>
			</Box>

			<Sheet
				open={sheetOpen}
				onClose={handleTestSheetClose}
			>
				<Box px="md" py="md">
					<Text fz={20} fw="bold">
						Hello from the sheet!
					</Text>
					<Text fz={15} c="TextDimmed">
						Drag down to dismiss. Drag up to expand to fullscreen.
						Try scrolling this content.
					</Text>
					<Box py={16} />
					{[...Array(20)].map((_, i) => (
						<Box key={i} py={8}>
							<Text fz={14}>
								Item {i + 1}
							</Text>
						</Box>
					))}
				</Box>
			</Sheet>
		</Container>
	);
}
