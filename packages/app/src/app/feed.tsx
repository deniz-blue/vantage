import { useLocalSearchParams, useRouter } from "expo-router";
import { Box } from "../components/base/Box";
import { Select } from "../components/base/input/Select";
import { useMemo, useState } from "react";
import { TextInput } from "../components/base/input/TextInput";
import { Text } from "../components/base/Text";
import { FontSize, IconSize } from "../theme/sizing";
import { Colors } from "../theme/colors";
import { SafeAreaView } from "react-native-safe-area-context";
import { Button } from "../components/base/button/Button";
import { Feed } from "../components/feed/feed";
import { JsonFeedPage } from "../components/feed/json-feed";
import { AtprotoDid, isActorIdentifier } from "@atcute/lexicons/syntax";
import { useAtAccounts } from "@vantage/atproto";
import { AtUserAvatar, AtUserHandle } from "../components/user/AtUserCard";
import { AtFeedPage } from "../components/feed/at-feed";

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
			{params.type === "at" && <AtFeedPage feed={params as Feed.Info<"at">} />}
		</Box>
	);
}

export const NoneFeedPage = ({ onViewFeed }: { onViewFeed?: (feed: Feed.Info) => void }) => {
	const accounts = useAtAccounts((s) => s.accounts);
	const [feed, setFeed] = useState<Feed.Info>({
		type: "jsonfeed",
		url: "",
	});

	const valid = useMemo(() => {
		if (feed.type === "jsonfeed") {
			try {
				const url = new URL(feed.url);
				return url.protocol === "http:" || url.protocol === "https:";
			} catch {
				return false;
			}
		}

		if (feed.type === "at") return isActorIdentifier(feed.repo);
	}, [feed]);

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
							setFeed({ type, repo: "" as any, collection: "directory.evnt.event" });
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

			{feed.type === "at" && (
				<>
					<TextInput
						label="Handle or DID"
						value={feed.repo}
						onChangeText={(repo) => setFeed({ ...feed, repo } as any)}
						placeholder="alice.bsky.social"
					/>

					{Object.keys(accounts).map((did) => (
						<Button
							key={did}
							onPress={() => setFeed({ ...feed, repo: did } as any)}
							justify="flex-start"
							leftSection={<AtUserAvatar did={did as AtprotoDid} size={IconSize.sm} />}
						>
							<Text>
								<AtUserHandle did={did as AtprotoDid} />
							</Text>
						</Button>
					))}
				</>
			)}

			<Button disabled={!valid} onPress={() => onViewFeed?.(feed)}>
				View Feed
			</Button>
		</Box>
	);
};
