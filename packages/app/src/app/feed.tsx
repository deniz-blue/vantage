import { useLocalSearchParams, useRouter } from "expo-router";
import { Box } from "../components/base/Box";
import { Select } from "../components/base/input/Select";
import { useCallback, useState } from "react";
import { TextInput } from "../components/base/input/TextInput";
import { useQuery } from "@tanstack/react-query";
import { Text } from "../components/base/Text";
import { FontSize } from "../theme/sizing";
import { Colors } from "../theme/colors";
import { FlatList } from "react-native-gesture-handler";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";
import { Button } from "../components/base/button/Button";
import { Card } from "../components/base/Card";
import { eventQueryFn, EventResolver, ResolvedEventContext } from "@vantage/core";
import { EventCard } from "../components/event/card/EventCard";
import { ButtonBase } from "../components/base/ButtonBase";
import { ViewToken } from "react-native";
import { Spacing } from "../theme/spacing";

export namespace Feed {
	export interface FeedTypeMap {
		jsonfeed: {
			url: string;
		};
	}

	export type FeedInfo = {
		[K in keyof FeedTypeMap]: {
			type: K;
		} & FeedTypeMap[K];
	}[keyof FeedTypeMap];

	export type Feed<T extends keyof FeedTypeMap = keyof FeedTypeMap> = {
		type: T;
	} & FeedTypeMap[T];

	export interface JsonFeedData {
		items: JsonFeedItem[];
	}

	export interface JsonFeedItem {
		id: string;
		url?: string;
		title?: string;
		content_text?: string;
	}
}

export default function FeedPage() {
	const params: Partial<Feed.FeedInfo> = useLocalSearchParams();
	const router = useRouter();

	const handleViewFeed = (feed: Feed.FeedInfo) => {
		router.push(`/feed?` + new URLSearchParams(feed).toString());
	};

	return (
		<Box>
			{!params.type && <NoneFeedPage onViewFeed={handleViewFeed} />}
			{params.type === "jsonfeed" && <JsonFeedPage feed={params as Feed.Feed<"jsonfeed">} />}
		</Box>
	);
}

export const NoneFeedPage = ({ onViewFeed }: { onViewFeed?: (feed: Feed.FeedInfo) => void }) => {
	const [feed, setFeed] = useState<Feed.FeedInfo>({
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

			<Select label="Feed Type" data={["jsonfeed"]} onChange={() => {}} value={feed.type} />

			{feed.type === "jsonfeed" && (
				<TextInput
					label="Feed URL"
					value={feed.url}
					onChangeText={(url) => setFeed({ ...feed, url })}
					placeholder="https://example.com/feed.json"
				/>
			)}

			<Button onPress={() => onViewFeed?.(feed)}>View Feed</Button>
		</Box>
	);
};

export const JsonFeedPage = ({ feed }: { feed: Feed.Feed<"jsonfeed"> }) => {
	const [visibleIds, setVisibleIds] = useState<Set<string>>(new Set());
	const insets = useSafeAreaInsets();

	const onViewableItemsChanged = useCallback(
		({ viewableItems }: { viewableItems: Array<ViewToken<Feed.JsonFeedItem>> }) => {
			setVisibleIds(new Set(viewableItems.map((v) => v.item.id)));
		},
		[],
	);

	const feedQuery = useQuery({
		queryKey: ["jsonfeed", feed.url],
		queryFn: async () => {
			const data = (await fetch(feed.url).then((res) => res.json())) as Feed.JsonFeedData;

			const base = feed.url.replace(/\/[^/]*$/, "/");
			for (let item of data.items) {
				if (item.url && !item.url?.startsWith("https")) item.url = base + item.url;
				if (item.id && !item.id?.startsWith("https")) item.id = base + item.id;
			}

			return data;
		},
		enabled: feed.url.trim().length > 0,
		retry: false,
	});

	const filteredItems = feedQuery.data?.items.filter((item) => item.url) ?? [];

	return (
		<FlatList
			data={filteredItems}
			renderItem={({ item }) => <JsonFeedEvent item={item} isVisible={visibleIds.has(item.id)} />}
			keyExtractor={(item) => item.id || item.url!}
			onViewableItemsChanged={onViewableItemsChanged}
			extraData={visibleIds}
			contentContainerStyle={{
				paddingBottom: insets.bottom + Spacing.md,
				paddingTop: insets.top + Spacing.md,
			}}
			ListHeaderComponent={
				<Box p="md">
					<Text fz={FontSize.sm} fw="bold">
						Feed URL:
					</Text>
					<Text fz={FontSize.xs} c={Colors.TextDimmed}>
						{feed.url}
					</Text>
				</Box>
			}
		/>
	);
};

export const JsonFeedEvent = ({
	item,
	isVisible = false,
}: {
	item: Feed.JsonFeedItem;
	isVisible?: boolean;
}) => {
	const router = useRouter();

	const query = useQuery({
		queryKey: ["event-from-url", item.url],
		enabled: isVisible,
		queryFn: () =>
			eventQueryFn(
				EventResolver.new({
					format: { type: "directory.evnt.event" },
					source: { type: "http", url: item.url ?? "" },
				}),
			),
	});

	const onPress = useCallback(() => {
		router.push(`/event?${new URLSearchParams({ url: item.url ?? "" }).toString()}`);
	}, [item.url]);

	return (
		<Box p="sm">
			{query.data?.data ? (
				<ResolvedEventContext value={query.data}>
					<EventCard onPress={onPress} />
				</ResolvedEventContext>
			) : (
				<ButtonBase onPress={onPress}>
					<JsonFeedEventItemCard item={item} />
				</ButtonBase>
			)}
		</Box>
	);
};

export const JsonFeedEventItemCard = ({ item }: { item: Feed.JsonFeedItem }) => {
	return (
		<Card>
			<Text fz={FontSize.sm} fw="bold">
				{item.title || item.url || item.id}
			</Text>
			{item.content_text && (
				<Text fz={FontSize.sm} c={Colors.TextDimmed}>
					{item.content_text}
				</Text>
			)}
		</Card>
	);
};
