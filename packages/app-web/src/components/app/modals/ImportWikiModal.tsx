import { Collapse, Stack, TextInput } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { useQuery } from "@tanstack/react-query";
import { eventQueryFnNoId, mediawiki, ResolvedEventContext } from "@vantage/core";
import { useState } from "react";
import { EventCard } from "../../content/event/card/EventCard";
import { Link } from "@tanstack/react-router";

export const ImportWikiModal = ({
	innerProps: { },
}: ContextModalProps<{}>) => {
	const [wikiUrl, setWikiUrl] = useState("https://eventswiki.deniz.blue");
	const [search, setSearch] = useState("");

	const pageTitles = useQuery({
		queryKey: ["mediawiki:search", wikiUrl, search],
		enabled: search.length > 0,
		staleTime: 5 * 60 * 1000, // 5 minutes
		placeholderData: (d) => d,
		queryFn: async (): Promise<string[]> => {
			return await mediawiki.searchPages(wikiUrl, search);
		},
	});

	return (
		<Stack>
			<TextInput
				label="Search EventsWiki"
				placeholder="Enter a search term"
				value={search}
				onChange={(e) => setSearch(e.currentTarget.value)}
				loading={pageTitles.isLoading}
				error={pageTitles.isError ? ("" + pageTitles.error.message) : undefined}
			/>

			<Collapse expanded={true}>
				<Stack>
					{pageTitles.data?.map((title) => (
						<PageTitleToEventCard
							key={title}
							wikiUrl={wikiUrl}
							title={title}
						/>
					))}
				</Stack>
			</Collapse>
		</Stack>
	);
}

export const PageTitleToEventCard = ({
	wikiUrl,
	title,
}: {
	wikiUrl: string;
	title: string;
}) => {
	const query = useQuery({
		queryKey: ["mw:resolved", wikiUrl, title],
		queryFn: async (): Promise<Vantage.ResolvedEvent> => {
			const source: Vantage.EventSource = {
				type: "mediawiki",
				url: wikiUrl,
				title,
			};

			const format: Vantage.EventFormat = { type: "directory.evnt.event" };

			return await eventQueryFnNoId(source, format);
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
							source: ""+(new URL(`rest.php/v1/page/${title}`, wikiUrl)),
						}}
						{...props}
					/>
				)}
			/>
		</ResolvedEventContext>
	)
};
