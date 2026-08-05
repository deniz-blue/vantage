import { useLocalSearchParams, useRouter } from "expo-router";
import { Box } from "../components/base/Box";
import { Select } from "../components/base/input/Select";
import { useState } from "react";
import { TextInput } from "../components/base/input/TextInput";
import { Text } from "../components/base/Text";
import { FontSize } from "../theme/sizing";
import { Colors } from "../theme/colors";
import { SafeAreaView } from "react-native-safe-area-context";
import { Button } from "../components/base/button/Button";
import { Feed } from "../components/feed/feed";
import { JsonFeedPage } from "../components/feed/json-feed";

export default function FeedPage() {
	const params: Partial<Feed.Info> = useLocalSearchParams();
	const router = useRouter();

	const handleViewFeed = (feed: Feed.Info) => {
		router.push(`/feed?` + new URLSearchParams(feed).toString());
	};

	return (
		<Box>
			{!params.type && <NoneFeedPage onViewFeed={handleViewFeed} />}
			{params.type === "jsonfeed" && <JsonFeedPage feed={params as Feed.Info<"jsonfeed">} />}
		</Box>
	);
}

export const NoneFeedPage = ({ onViewFeed }: { onViewFeed?: (feed: Feed.Info) => void }) => {
	const [feed, setFeed] = useState<Feed.Info>({
		type: "jsonfeed",
		url: "",
	});

	return (
		<Box component={SafeAreaView} gap="md" p="md">
			<Box align="center" my="md">
				<Text>Import from a Feed</Text>
				<Text fz={FontSize.sm} c={Colors.TextDimmed}>
					Pick events to import from a a Feed
				</Text>
			</Box>

			<Select
				label="Feed Type"
				data={["jsonfeed", "at"]}
				onChange={(type: keyof Feed.TypeMap) => {
					switch (type) {
						case "jsonfeed":
							setFeed({ type, url: "" });
							break;
						case "at":
							setFeed({ type, repo: "" });
							break;
					}
				}}
				value={feed.type}
			/>

			{feed.type === "jsonfeed" && (
				<TextInput
					label="Feed URL"
					value={(feed as Feed.Info<"jsonfeed">).url}
					onChangeText={(url) => setFeed({ ...feed, url })}
					placeholder="https://example.com/feed.json"
				/>
			)}

			<Button onPress={() => onViewFeed?.(feed)}>View Feed</Button>
		</Box>
	);
};
