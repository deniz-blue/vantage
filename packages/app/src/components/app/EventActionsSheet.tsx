import { usePathname, useRouter } from "expo-router";
import { Sheet, SheetRef } from "../base/sheet/Sheet";
import { EventsManager, useResolvedEvent } from "@vantage/core";
import { createActionsForEvent } from "../actions/event-actions";
import { IconPencil, IconRefresh, IconX } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { IconSize } from "../../theme/sizing";
import { ActionButtonList } from "../actions/ActionButton";
import { Box } from "../base/Box";
import { RefObject, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button } from "../base/button/Button";
import { Text } from "../base/Text";
import { AsyncButton } from "../base/button/AsyncButton";
import { TransText } from "../core/TransText";
import { useCanEditEvent } from "../../hooks/useCanEditEvent";

export const EventActionsSheet = ({ sheet }: { sheet: RefObject<SheetRef | null> }) => {
	const router = useRouter();
	const resolved = useResolvedEvent();
	const actions = [...createActionsForEvent(resolved)];
	const deleteConfirmSheet = useRef<SheetRef>(null);
	const pathname = usePathname();
	const canEdit = useCanEditEvent(resolved);

	const deleteMutation = useMutation({
		mutationFn: async () => {
			if (!resolved.id) throw new Error("No event ID");
			await EventsManager.removeEvent(resolved.id);
		},
		onSuccess: () => {
			sheet.current?.dismiss();
			deleteConfirmSheet.current?.dismiss();
			if (pathname.startsWith(`/event/${resolved.id}`)) {
				if (router.canGoBack()) router.back();
				else router.push("/");
			}
		},
	});

	if (canEdit)
		actions.push({
			label: "Edit",
			type: "fn",
			onRun: () => router.push(`/event/${resolved.id}/edit`),
			icon: <IconPencil size={IconSize.xs} color={Colors.Text} />,
		});

	if (resolved.id)
		actions.push({
			label: "Delete",
			type: "fn",
			onRun: () => deleteConfirmSheet.current?.present(),
			icon: <IconX size={IconSize.xs} color={Colors.Text} />,
			danger: true,
		});

	return (
		<Sheet ref={sheet}>
			<Box gap="sm">
				<TransText
					fw="bold"
					numberOfLines={1}
					value={resolved.data?.name}
					fallback={
						<Text fst="italic" c="TextDimmed">
							Untitled event
						</Text>
					}
				/>

				{resolved.id && (
					<AsyncButton fn={() => EventsManager.refetchEvent(resolved.id!)}>
						{({ loading, onPress }) => (
							<Button
								loading={loading}
								leftSection={<IconRefresh color={Colors.Text} size={IconSize.xs} />}
								justify="flex-start"
								onPress={onPress}
							>
								Refetch Event
							</Button>
						)}
					</AsyncButton>
				)}

				<ActionButtonList actions={actions} />
			</Box>

			<Sheet ref={deleteConfirmSheet}>
				<Box gap="sm">
					<Text>
						{resolved.source.type !== "local"
							? "Are you sure you want to stop following this event?"
							: "Are you sure you want to delete this event? This action cannot be undone."}
					</Text>
					<Button
						size="sm"
						variant="danger"
						children="Delete"
						onPress={() => deleteMutation.mutate()}
						loading={deleteMutation.isPending}
					/>
				</Box>
			</Sheet>
		</Sheet>
	);
};
