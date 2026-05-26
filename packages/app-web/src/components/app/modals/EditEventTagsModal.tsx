import { Badge, Button, Checkbox, Combobox, Group, Paper, ScrollArea, Stack, TextInput, useCombobox } from "@mantine/core";
import { modals, type ContextModalProps } from "@mantine/modals";
import { useMutation, useQuery } from "@tanstack/react-query";
import { EventTagManager, TagsManager, invalidateEventQuery } from "@vantage/core";
import { schema, db } from "@vantage/db";
import { eq } from "drizzle-orm";
import { sqlite } from "../../../db/drizzle";
import { notifications } from "@mantine/notifications";
import { useEffect, useState } from "react";

export const EditEventTagsModal = ({
	innerProps: { eventId },
	context,
	id,
}: ContextModalProps<{
	eventId: Vantage.EventId;
}>) => {
	const tags = useQuery({
		queryKey: ["event-tags", eventId],
		enabled: !!eventId,
		queryFn: async () => {
			return await EventTagManager.getTagsForEvent(eventId);
		},
	});

	const allTags = useQuery({
		queryKey: ["tags", "all"],
		queryFn: async () => {
			return await TagsManager.getAll();
		},
	});

	const [selected, setSelected] = useState<string[]>([]);
	const [search, setSearch] = useState("");

	useEffect(() => {
		if (tags.data) setSelected(tags.data.map(t => t.id));
	}, [tags.data]);

	const filteredTags = allTags.data?.filter(tag => tag.name.toLowerCase().includes(search.trim().toLowerCase())) ?? [];

	const toggleTag = (tagId: string) => {
		setSelected(current => (
			current.includes(tagId)
				? current.filter(currentTagId => currentTagId !== tagId)
				: [...current, tagId]
		));
	};

	const mut = useMutation({
		mutationFn: async (tagIds: string[]) => {
			if (!eventId) throw new Error("Missing event id");
			await sqlite.transaction(async tx => {
				await tx.query(db.delete(schema.eventTags).where(eq(schema.eventTags.eventId, eventId)));
				if (tagIds.length) {
					const values = tagIds.map(tagId => ({ eventId, tagId }));
					await tx.query(db.insert(schema.eventTags).values(values));
				}
			});
			invalidateEventQuery(eventId);
		},
		onSuccess: () => {
			notifications.show({ title: "Saved", message: "Event tags updated", color: "green" });
			context.closeModal(id);
		},
		onError: (err: any) => {
			notifications.show({ title: "Error", message: String(err?.message ?? err), color: "red" });
		},
	});

	const combobox = useCombobox();

	useEffect(() => {
		combobox.selectFirstOption();
	}, [search]);

	return (
		<Stack>
			<Combobox
				store={combobox}
				onOptionSubmit={(id) => toggleTag(id)}
			>
				<Group gap={4} align="end">
					<Combobox.EventsTarget>
						<TextInput
							flex="1"
							label="Tags"
							placeholder="Search tags..."
							value={search}
							onChange={(event) => {
								let s = event.currentTarget.value;
								if (!s.trim() && !search) combobox.clickSelectedOption();
								else setSearch(s);
							}}
							disabled={allTags.isLoading}
						/>
					</Combobox.EventsTarget>
					<Button
						onClick={() => modals.openContextModal({
							modal: "EditTagModal",
							innerProps: {},
						})}
					>
						New Tag
					</Button>
				</Group>
				<ScrollArea.Autosize mah={240}>
					<Combobox.Options>
						{filteredTags.length ? filteredTags.map(tag => {
							const active = selected.includes(tag.id);

							return (
								<Combobox.Option
									key={tag.id}
									value={tag.id}
									active={active}
								>
									<Group justify="space-between" wrap="nowrap">
										<Group gap="xs" wrap="nowrap">
											<Checkbox
												checked={active}
												readOnly
												tabIndex={-1}
												onChange={() => { }}
												aria-hidden
												style={{ pointerEvents: "none" }}
											/>
											<Badge color={tag.color ?? "gray"} variant="light" tt="unset">
												{tag.name}
											</Badge>
										</Group>
									</Group>
								</Combobox.Option>
							);
						}) : (
							<Combobox.Empty>Nothing found</Combobox.Empty>
						)}
					</Combobox.Options>
				</ScrollArea.Autosize>
			</Combobox>

			<Group justify="space-between">
				<Button
					color="gray"
					onClick={() => context.closeModal(id)}
				>
					Cancel
				</Button>

				<Button
					color="green"
					onClick={() => mut.mutate(selected)}
					loading={mut.isPending}
				>
					Save
				</Button>
			</Group>
		</Stack>
	);
}
