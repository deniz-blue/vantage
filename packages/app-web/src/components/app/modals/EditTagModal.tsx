import { Button, ColorInput, DEFAULT_THEME, Group, Paper, Stack, Text, TextInput } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { notifications } from "@mantine/notifications";
import { useMutation, useQuery } from "@tanstack/react-query";
import { queryClient, TagsManager } from "@vantage/core";
import { useEffect, useMemo, useState } from "react";
import { TagBadge } from "../../content/event/badges/TagBadge";

export const EditTagModal = ({
	innerProps: { tagId },
	context,
	id,
}: ContextModalProps<{
	tagId?: Vantage.EventId;
}>) => {
	const tag = useQuery({
		queryKey: ["tag", tagId],
		enabled: !!tagId,
		queryFn: async () => {
			if (!tagId) return null;
			return await TagsManager.getById(tagId);
		},
	});

	const [name, setName] = useState("");
	const [color, setColor] = useState("");

	useEffect(() => {
		if (!tag.data) return;
		setName(tag.data.name);
		setColor(tag.data.color ?? "");
	}, [tag.data]);

	const mut = useMutation({
		mutationFn: async () => {
			if (!name.trim()) throw new Error("Tag name is required");

			if (tagId) {
				await TagsManager.update(tagId, {
					name: name.trim(),
					color: color.trim() || null,
				});
				return;
			}

			await TagsManager.create({
				name: name.trim(),
				color: color.trim() || null,
			});
		},
		onSuccess: async () => {
			await queryClient.invalidateQueries({ queryKey: ["tags", "all"] });
			if (tagId) {
				await queryClient.invalidateQueries({ queryKey: ["tag", tagId] });
				await queryClient.invalidateQueries({ predicate: query => query.queryKey[0] === "event" && query.queryKey[2] === "tags" });
			}
			notifications.show({
				title: "Saved",
				message: tagId ? "Tag updated" : "Tag created",
				color: "green",
			});
			context.closeModal(id);
		},
		onError: (err: any) => {
			notifications.show({ title: "Error", message: String(err?.message ?? err), color: "red" });
		},
	});

	const previewId = useMemo(() => tagId ?? crypto.randomUUID(), [tagId]);

	return (
		<Stack>
			<Group justify="space-between" align="center" gap={4}>
				<Text fw={600}>
					{tagId ? "Edit Tag" : "Create Tag"}
				</Text>

				<TagBadge
					tag={{ name, color, id: previewId, updatedAt: Temporal.Now.instant() }}
					readonly
				/>
			</Group>

			<TextInput
				label="Name"
				placeholder="Tag name"
				value={name}
				onChange={(event) => setName(event.currentTarget.value)}
				disabled={tag.isLoading}
			/>

			<ColorInput
				label="Color"
				format="hex"
				value={color}
				onChange={(c) => setColor(c)}
				disabled={tag.isLoading}
				swatches={Object.values(DEFAULT_THEME.colors).map(x => x[7]!)}
			/>

			<Group justify="space-between">
				<Button
					variant="light"
					color="gray"
					onClick={() => context.closeModal(id)}
				>
					Cancel
				</Button>

				<Button
					onClick={() => mut.mutate()}
					loading={mut.isPending}
					disabled={!!tagId && !tag.data}
				>
					Save
				</Button>
			</Group>
		</Stack>
	);
};