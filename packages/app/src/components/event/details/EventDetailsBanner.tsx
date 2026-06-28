import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { ControlHeight, FontSize } from "../../../theme/sizing";
import { useRouter } from "expo-router";
import { ActionIcon } from "../../base/ActionIcon";
import { IconArrowLeft } from "@tabler/icons-react-native";

export const EventDetailsBanner = () => {
	const router = useRouter();
	const { data } = useResolvedEvent();

	return (
		<Box
			direction="row"
			gap="sm"
			py="md"
			px="md"
			style={{
				shadowColor: "#000",
				shadowOffset: { width: 0, height: 2 },
				shadowOpacity: 0.15,
				shadowRadius: 6,
				elevation: 4,
			}}
		>
			{router.canGoBack() && (
				<ActionIcon
					variant="subtle"
					onPress={() => router.back()}
					size="auto"
				>
					<IconArrowLeft size={20} />
				</ActionIcon>
			)}
			<Box flex={1} gap={4}>
				<TransText
					fz={FontSize.h1}
					fw="bold"
					value={data?.name}
					fallback={<Text fz={16} fst="italic" c="TextDimmed">Untitled event</Text>}
				/>
				{data?.label && (
					<TransText fz={FontSize.md} c="TextDimmed" value={data.label} />
				)}
			</Box>
		</Box>
	);
};
