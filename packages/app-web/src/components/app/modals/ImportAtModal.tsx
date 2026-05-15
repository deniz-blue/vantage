import { ComAtprotoRepoListRecords } from "@atcute/atproto";
import { AtprotoDid, CanonicalResourceUri } from "@atcute/lexicons/syntax";
import { Avatar, Button, Collapse, Divider, Group, Select, Stack, Text } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { searchActorsTypeahead, useInfiniteRepoListRecords } from "@vantage/atproto";
import { eventQueryFnNoId, parseEventFormat, ResolvedEventContext } from "@vantage/core";
import { useState } from "react";
import { EventCard } from "../../content/event/card/EventCard";
import { Link } from "@tanstack/react-router";

export const ImportAtModal = ({
	innerProps: { },
}: ContextModalProps<{}>) => {
	const [search, setSearch] = useState("");
	const [selectedDid, setSelectedDid] = useState<AtprotoDid | null>(null);

	const users = useQuery({
		queryKey: ["typeahead", search],
		enabled: search.length > 0,
		staleTime: 60 * 60 * 1000, // 1 hour
		placeholderData: (d) => d,
		queryFn: async () => {
			const res = await searchActorsTypeahead(search, { limit: 5 });
			if (!res.ok) throw new Error(res.data.message || res.data.error);
			return res.data.actors;
		},
	});

	const renderOption: Select.Props['renderOption'] = ({ option }) => (
		<Group gap="sm">
			<Avatar src={`https://blobs.blue/${option.value}/avatar-thumb`} size={36} radius="xl" />
			<div>
				<Text size="sm">{users.data?.find((u) => u.did === option.value)?.displayName}</Text>
				<Text size="xs" opacity={0.5}>
					{users.data?.find((u) => u.did === option.value)?.handle}
				</Text>
			</div>
		</Group>
	);

	const openevnt = useInfiniteRepoListRecords(selectedDid, "directory.evnt.event");
	const communitylexicon = useInfiniteRepoListRecords(selectedDid, "community.lexicon.calendar.event");

	return (
		<Stack>
			<Select<AtprotoDid>
				label="Search Atmosphere users"
				placeholder="Enter a handle"
				maxDropdownHeight={300}
				data={users.data?.map((user) => ({
					value: user.did as AtprotoDid,
					label: user.handle,
				})) || []}
				value={selectedDid}
				onChange={setSelectedDid}
				loading={users.isLoading}
				error={users.isError ? ("" + users.error.message) : undefined}
				searchable
				searchValue={search}
				onSearchChange={setSearch}
				renderOption={renderOption}
			/>

			<Collapse expanded={true}>
				<Stack>
					{[openevnt, communitylexicon].map((query, i) => (
						<Stack
							key={i}
						>
							{((query.data?.pages.length ?? 0) > 0 && (query.data?.pages[0]?.records.length ?? 0) > 0) && (
								<Divider my="xs" label={i === 0 ? "OpenEvnt" : "Lexicon Community"} />
							)}

							{query.data?.pages.map((page, j) => (
								<Stack key={j}>
									{page.records.map((record) => (
										<RecordToEventCard
											key={record.uri}
											record={record}
											format={i === 0 ? { type: "directory.evnt.event" } : { type: "community.lexicon.calendar.event" }}
										/>
									))}
								</Stack>
							))}

							<Collapse expanded={query.hasNextPage}>
								<Button
									loading={query.isFetchingNextPage}
									onClick={() => query.fetchNextPage()}
									fullWidth
								>
									Load more
								</Button>
							</Collapse>
						</Stack>
					))}
				</Stack>
			</Collapse>
		</Stack>
	);
}

export const RecordToEventCard = ({
	record,
	format,
}: {
	record: ComAtprotoRepoListRecords.Record;
	format: Vantage.EventFormat;
}) => {
	const query = useQuery({
		queryKey: ["at:resolved", record.uri],
		queryFn: async (): Promise<Vantage.ResolvedEvent> => {
			const raw = JSON.stringify(record.value);
			const parsed = parseEventFormat(raw, format);
			return {
				id: null,
				data: parsed.parsed,
				raw,
				error: parsed.error,
				source: {
					type: "at",
					uri: record.uri as CanonicalResourceUri,
				},
				format,
				revision: {
					cid: record.cid,
				},
			};
		},
	});

	return (
		<ResolvedEventContext
			value={query.data ?? null}
		>
			<EventCard
				loading={query.isLoading}
				variant="horizontal"
				renderRoot={(props) => (
					<Link
						to="/event"
						search={{
							source: record.uri,
						}}
						{...props}
					/>
				)}
			/>
		</ResolvedEventContext>
	)
};
