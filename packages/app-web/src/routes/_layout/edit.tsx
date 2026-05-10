import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { FormPageTemplate } from "../form";
import { Alert, Stack } from "@mantine/core";
import { useATProtoAuthStore } from "../../lib/atproto/useATProtoStore";
import { useSetAtom } from "jotai";
import { atom } from "jotai";
import type { EventData } from "@evnt/schema";
import { useEffect, useMemo } from "react";
import { useEventQuery } from "../../db/useEventQuery";
import { useMutation } from "@tanstack/react-query";
import z from "zod";
import { useEditorCollapseState } from "../../components/editor/editor-states";
import { eventMutationFn } from "../../db/useEventMutation";

const RouteSearchSchema = z.object({
	id: z.uuid() as z.ZodType<Vantage.EventId>,
});

export const Route = createFileRoute("/_layout/edit")({
	component: EditPage,
	validateSearch: RouteSearchSchema,
	staticData: {
		hasEventForm: true,
	},
})

function EditPage() {
	const { id } = Route.useSearch();
	const signedInDid = useATProtoAuthStore(store => store.agent?.sub);

	const dataAtom = useMemo(() => atom<EventData | null>(null), []);
	const setDataAtom = useSetAtom(dataAtom);

	const query = useEventQuery(id);

	useEffect(() => {
		if (!query?.data) return;
		setDataAtom(prev => prev ?? query.data?.data ?? null);

		// ???? -deniz @ 2026-05-10
		if (query.data?.data) {
			useEditorCollapseState.setState({
				collapsed: [
					...query.data.data.venues?.map(venue => `venue::${venue.id}`) ?? [],
					...query.data.data.instances?.map((_, i) => `instance::${i}`) ?? [],
					...query.data.data.components?.map((_, i) => `component::${i}`) ?? [],
				],
			});
		}
	}, [query?.data ?? null]);

	// ??? -deniz @ 2026-05-10
	useEffect(() => () => useEditorCollapseState.setState({ collapsed: [] }), []);

	const navigate = useNavigate();

	const mutation = useMutation({
		mutationFn: eventMutationFn,
		onSuccess: async (_, { id }) => {
			navigate({
				to: "/event",
				search: {
					id,
				},
			})
		},
	});

	const save = useSetAtom(useMemo(() => atom(null, async (get, set) => {
		const data = get(dataAtom);
		console.log("Saving data", data);
		mutation.mutate({
			id,
			raw: JSON.stringify(data),
		});
	}), [dataAtom, mutation]));

	const loading = (mutation.isPending || query?.isLoading);

	return (
		<FormPageTemplate
			title="Edit Event"
			continueText="Save"
			onContinue={save}
			loading={loading}
			data={dataAtom}
			notice={(
				<Stack>
					{query.data?.source.type == "at" && !signedInDid && (
						<Alert
							title="Not signed in"
							color="yellow"
							my="md"
						>
							<Stack gap={4}>
								You are not signed in to ATProto!
							</Stack>
						</Alert>
					)}
				</Stack>
			)}
		/>
	)
}
