import { Select, Stack } from "@mantine/core";
import { queryClient, useResolvedEvent } from "@vantage/core";
import { SmallTitle } from "../../base/SmallTitle";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Client, ok, simpleFetchHandler } from "@atcute/client";
import type { } from "@atcute/microcosm";
import type { CommunityLexiconCalendarRsvp } from "@atcute/lexicon-community";
import { useATProtoAuthStore } from "../../../../lib/atproto/useATProtoStore";
import { repoGetRecord } from "@vantage/atproto";
import { AtprotoDid, parseCanonicalResourceUri } from "@atcute/lexicons/syntax";
import { notifications } from "@mantine/notifications";

export const EventDetailsRSVP = () => {
	const { source } = useResolvedEvent();
	const { agent, rpc } = useATProtoAuthStore();

	const COLLECTION = "community.lexicon.calendar.rsvp";
	type Status = CommunityLexiconCalendarRsvp.Notgoing | CommunityLexiconCalendarRsvp.Interested | CommunityLexiconCalendarRsvp.Going | "";

	const rsvp = useQuery({
		queryKey: ["rsvp", agent?.sub, source.type === "at" ? source.uri : null],
		enabled: source.type === "at" && !!agent,
		queryFn: async () => {
			if (source.type !== "at") throw new Error("Invalid source type");
			if (!agent) throw new Error("Not authenticated");

			const client = new Client({
				handler: simpleFetchHandler({
					service: "https://constellation.microcosm.blue/",
				}),
			});

			const backlink = ok(await client.get("blue.microcosm.links.getBacklinks", {
				params: {
					subject: source.uri,
					did: [agent.sub],
					source: "community.lexicon.calendar.rsvp:subject.uri",
				},
			}));

			if (!backlink.records[0]) return null;
			const { did, collection, rkey } = backlink.records[0];

			return ok(await repoGetRecord(did as AtprotoDid, collection, rkey));
		},
	});

	const mut = useMutation({
		mutationKey: ["rsvp", agent?.sub, source.type === "at" ? source.uri : null],
		mutationFn: async (status: Status) => {
			if (source.type !== "at") throw new Error("Invalid source type");
			if (!agent || !rpc) throw new Error("Not authenticated");

			const record = {
				subject: source.uri,
				status,
			};

			if (!status) {
				if (!rsvp.data) return null;
				const { repo, collection, rkey } = parseCanonicalResourceUri(rsvp.data.uri);
				await rpc.post("com.atproto.repo.deleteRecord", {
					input: {
						repo,
						collection,
						rkey,
					},
				});
				return null;
			} else if (rsvp.data) {
				const { repo, collection, rkey } = parseCanonicalResourceUri(rsvp.data.uri);
				await rpc.post("com.atproto.repo.putRecord", {
					input: {
						repo,
						collection,
						rkey,
						record,
					},
				});
				return { record, uri: `at://${repo}/${collection}/${rkey}` };
			} else {
				const res = ok(await rpc.post("com.atproto.repo.createRecord", {
					input: {
						repo: agent.sub,
						collection: COLLECTION,
						record,
					},
				}));
				return { record, uri: `at://${agent.sub}/${COLLECTION}/<generated>` };
			};
		},
		onSuccess: async (done) => {
			notifications.show({
				title: "RSVP updated",
				message: "Your RSVP status has been updated.",
				color: "green",
				autoClose: 3000,
			});

			await queryClient.setQueryData(["rsvp", agent?.sub, source.type === "at" ? source.uri : null], done ? {
				value: done.record,
				uri: done.uri,
				cid: null, // eeeh
			} : null);
		},
		onError: (error) => {
			notifications.show({
				title: "Error updating RSVP",
				message: error instanceof Error ? error.message : "An unknown error occurred.",
				color: "red",
				autoClose: 5000,
			});
		},
	});

	if (source.type !== "at") return null;

	const record = rsvp.data?.value as CommunityLexiconCalendarRsvp.Main | null;

	return (
		<Stack gap={0} component="section">
			<SmallTitle>
				rsvp
			</SmallTitle>
			<Stack gap={0}>
				<Select<Status>
					value={(record?.status as any) ?? ""}
					onChange={(value) => {
						mut.mutate(value ?? "");
					}}
					clearable
					data={[
						{ value: "community.lexicon.calendar.rsvp#going", label: "Going" },
						{ value: "community.lexicon.calendar.rsvp#interested", label: "Interested" },
						{ value: "community.lexicon.calendar.rsvp#notgoing", label: "Not Going" },
						{ value: "", label: "None" },
					]}
					loading={rsvp.isLoading || mut.isPending}
				/>
			</Stack>
		</Stack>
	);
};
