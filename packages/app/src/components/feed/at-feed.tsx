import { useCallback } from "react";
import { Feed } from "./feed";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useQuery } from "@tanstack/react-query";
import { FlatList } from "react-native";
import { Spacing } from "../../theme/spacing";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { useRouter } from "expo-router";
import { EventResolver, ResolvedEventContext } from "@vantage/core";
import { EventCard } from "../event/card/EventCard";
import { identityResolver, useInfiniteRepoListRecords } from "@vantage/atproto";
import {
	AtprotoDid,
	CanonicalResourceUri,
	parseCanonicalResourceUri,
} from "@atcute/lexicons/syntax";
import { AtUserAvatar } from "../user/AtUserCard";
import { ComAtprotoRepoListRecords } from "@atcute/atproto";

export const AtFeedPage = ({ feed }: { feed: Feed.Info<"at"> }) => {
	const insets = useSafeAreaInsets();

	const identity = useQuery({
		queryKey: ["atproto", "identity", feed.repo],
		queryFn: () => identityResolver.resolve(feed.repo),
	});

	const records = useInfiniteRepoListRecords(
		identity.data?.did as AtprotoDid | undefined,
		feed.collection,
	);

	return (
		<FlatList
			data={records.data?.pages.flatMap((page) => page.records) ?? []}
			renderItem={({ item }) => <AtFeedEvent item={item} />}
			keyExtractor={(item) => item.uri}
			contentContainerStyle={{
				paddingBottom: insets.bottom + Spacing.md,
				paddingTop: insets.top + Spacing.md,
			}}
			ListHeaderComponent={
				<Box p="md">
					{identity.data && (
						<Box direction="row" align="center" gap="sm">
							<AtUserAvatar did={identity.data.did as AtprotoDid} />
							<Box direction="row">
								<Text fw="bold">{identity.data.handle}</Text>
								<Text>'s Events</Text>
							</Box>
						</Box>
					)}
				</Box>
			}
		/>
	);
};

export const AtFeedEvent = ({ item }: { item: ComAtprotoRepoListRecords.Record }) => {
	const router = useRouter();

	const onPress = useCallback(() => {
		router.push(`/event?${new URLSearchParams({ at: item.uri ?? "" }).toString()}`);
	}, [item.uri]);

	const query = useQuery({
		queryKey: ["event-from-at", item.uri],
		queryFn: async () => {
			return await EventResolver.parse(
				EventResolver.new({
					raw: JSON.stringify(item.value),
					format: {
						type: parseCanonicalResourceUri(item.uri).collection as
							| "directory.evnt.event"
							| "community.lexicon.calendar.event",
					},
					source: { type: "at", uri: item.uri as CanonicalResourceUri },
				}),
			);
		},
	});

	return (
		<Box p="sm">
			<ResolvedEventContext value={query.data ?? null}>
				<EventCard onPress={onPress} />
			</ResolvedEventContext>
		</Box>
	);
};
