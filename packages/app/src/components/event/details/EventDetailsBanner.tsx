import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { FontSize } from "../../../theme/sizing";
import { useRouter } from "expo-router";
import { ActionIcon } from "../../base/button/ActionIcon";
import { IconArrowLeft } from "@tabler/icons-react-native";
import { ActivityIndicator } from "react-native";
import { Colors } from "../../../theme/colors";

export const EventDetailsBanner = ({ loading }: { loading?: boolean }) => {
	const router = useRouter();
	const { data } = useResolvedEvent();

	return (
		<Box
			direction="row"
			gap="sm"
			py="md"
			px="md"
			bg={Colors.Background}
			style={{
				shadowColor: "#000",
				shadowOffset: { width: 0, height: 2 },
				shadowOpacity: 0.15,
				shadowRadius: 6,
				elevation: 4,
			}}
		>
			<ActionIcon
				variant="subtle"
				onPress={() => router.canGoBack() ? router.back() : router.push("/")}
				size="auto"
			>
				<IconArrowLeft size={20} />
			</ActionIcon>
			{loading && (
				<ActivityIndicator />
			)}
			<Box flex={1} gap={4}>
				<TransText
					fz={FontSize.h1}
					fw="bold"
					value={data?.name}
					fallback={(
						<Text fz={16} fst="italic" c="TextDimmed">
							{loading ? "Loading…" : "Untitled"}
						</Text>
					)}
				/>
				{data?.label && (
					<TransText fz={FontSize.md} c="TextDimmed" value={data.label} />
				)}
			</Box>
		</Box>
	);
};
