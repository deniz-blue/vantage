import { OpenEvnt } from "@evnt/types";
import { Container } from "../../components/base/Container";
import { EventForm } from "../../components/event/editor/EventForm";
import { useEditor } from "../../components/event/editor/useEditor";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { FontSize } from "../../theme/sizing";
import { Select } from "../../components/base/Select";
import { Divider } from "../../components/base/Divider";
import { Button } from "../../components/base/Button";
import { ScrollView } from "react-native";

export default function NewEventPage() {
	const { editor } = useEditor((): OpenEvnt => ({ v: "0.1", name: {} }));

	return (
		<Box flex={1}>
			<Box component={ScrollView}>
				<Container size="sm" flex={1}>
					<Box py="md" flex={1}>
						<Box gap="md" flex={1}>
							<Box>
								<Text fz={FontSize.h1} fw="bold">
									New Event
								</Text>
							</Box>

							<Box>
								<Select
									label="Save to..."
									value="local"
									onChange={() => { }}
									data={[
										"local",
									]}
									renderItem={(item) => {
										if (item === "local") return "This Device";
										return item;
									}}
								/>
							</Box>

							<Divider />

							<EventForm editor={editor} />
						</Box>
					</Box>
				</Container>
			</Box>
			<Box pos="absolute" style={{ bottom: 0 }} w="100%">
				<Container size="sm" flex={1} pb="md">
					<Button variant="filled" color="Primary" w="100%" onPress={() => { }}>
						Save
					</Button>
				</Container>
			</Box>
		</Box>
	);
}
