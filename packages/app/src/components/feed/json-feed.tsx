import { useCallback, useState } from "react";
import { Feed } from "./feed";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useQuery } from "@tanstack/react-query";
import { FlatList, ViewToken } from "react-native";
import { Spacing } from "../../theme/spacing";
import { Box } from "../base/Box";
import { FontSize } from "../../theme/sizing";
import { Colors } from "../../theme/colors";
import { Text } from "../base/Text";
import { useRouter } from "expo-router";
import { eventQueryFn, EventResolver, ResolvedEventContext } from "@vantage/core";
import { EventCard } from "../event/card/EventCard";
import { ButtonBase } from "../base/ButtonBase";
import { Card } from "../base/Card";

export namespace JsonFeed {
	export interface Data {
		items: Item[];
	}

	export interface Item {
		id: string;
		url?: string;
		title?: string;
		content_text?: string;
	}
}

export const JsonFeedPage = ({ feed }: { feed: Feed.Info<"jsonfeed"> }) => {
	const [visibleIds, setVisibleIds] = useState<Set<string>>(new Set());
	const insets = useSafeAreaInsets();

	const onViewableItemsChanged = useCallback(
		({ viewableItems }: { viewableItems: Array<ViewToken<JsonFeed.Item>> }) => {
			setVisibleIds(new Set(viewableItems.map((v) => v.item.id)));
		},
		[],
	);

	const feedQuery = useQuery({
		queryKey: ["jsonfeed", feed.url],
		queryFn: async () => {
			const data = (await fetch(feed.url).then((res) => res.json())) as JsonFeed.Data;

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
	item: JsonFeed.Item;
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

export const JsonFeedEventItemCard = ({ item }: { item: JsonFeed.Item }) => {
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
