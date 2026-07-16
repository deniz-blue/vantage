import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { useHomeStore } from "../../stores/useHomeStore";
import { HomeWidget } from "../../components/home/HomeWidget";
import { Container } from "../../components/base/Container";
import { SafeAreaView } from "react-native-safe-area-context";
import { ModalBottomSheet } from "@swmansion/react-native-bottom-sheet";
import { Button } from "../../components/base/button/Button";
import { useState } from "react";
import { TextInput } from "../../components/base/input/TextInput";

export default function IndexPage() {
	const widgets = useHomeStore((s) => s.widgets);

	return (
		<Box component={SafeAreaView} flex={1}>
			<Container size="lg" flex={1} px={0} gap="md">
				<Box px="md" pt="md" pb={4}>
					<Text fz={24} fw="bold">
						Vantage
					</Text>
					<Text fz={13} c="TextDimmed">
						An open-source event calendar for everyone
					</Text>
				</Box>

				{widgets.map((widget, index) => (
					<HomeWidget key={index} widget={widget} />
				))}

				{/* <Test /> */}
			</Container>
		</Box>
	);
}

export const Test = () => {
	const [open, setOpen] = useState(true);

	return (
		<>
			<Button onPress={() => setOpen(true)}>Open</Button>
			<ModalBottomSheet
				detents={[0, "content"]}
				index={open ? 1 : 0}
				onIndexChange={(index) => {
					if (index === 0) setOpen(false);
				}}
				scrimColor="rgba(0,0,0,0.5)"
				surface={
					<Box
						absoluteFill
						bg="Background"
						style={{ borderTopLeftRadius: 16, borderTopRightRadius: 16 }}
					/>
				}
				animateContentHeight={false}
			>
				{/* <KeyboardAvoidingView behavior="padding"> */}
					<SafeAreaView>
						<TextInput placeholder="Test" />
						<Text>meow</Text>
					</SafeAreaView>
				{/* </KeyboardAvoidingView> */}
			</ModalBottomSheet>
		</>
	);
};
