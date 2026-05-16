import { createFileRoute } from "@tanstack/react-router"
import { EventFormatRegistry, EventSourceRegistry, useEventQueries } from "@vantage/core";
import { Accordion, ActionIcon, Autocomplete, Button, CloseButton, Group, Input, OverflowList, Paper, Popover, SegmentedControl, Stack, TextInput, Tooltip } from "@mantine/core";
import { EventsGrid } from "../../components/content/event-grid/EventsGrid";
import { zodValidator } from "@tanstack/zod-adapter";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { useEventListQuery } from "@vantage/core";
import z from "zod";
import { IconFilterOff } from "@tabler/icons-react";

const SearchParamsSchema = z.object({
	search: z.string().optional(),
	relativity: z.enum(["past", "future", "all"]).optional(),
	sortBy: z.enum(["name", "instanceStart", "none"]).optional(),
	hasError: z.boolean().optional(),
	sourceType: z.string().optional(),
	formatType: z.string().optional(),
});

const defaultSearchParams = {
	search: "",
	relativity: "future" as const,
	sortBy: "instanceStart" as const,
	formatType: "",
	sourceType: "",
	hasError: undefined,
} satisfies z.infer<typeof SearchParamsSchema>;

export const Route = createFileRoute("/_layout/list")({
	component: ListPage,
	validateSearch: zodValidator(SearchParamsSchema),
})

type SortBy = "name" | "instanceStart" | "none";

function ListPage() {
	const searchObject = Route.useSearch();
	const {
		search = defaultSearchParams.search,
		relativity = defaultSearchParams.relativity,
		sortBy = defaultSearchParams.sortBy,
		formatType = defaultSearchParams.formatType,
		sourceType = defaultSearchParams.sourceType,
		hasError = defaultSearchParams.hasError,
	} = searchObject;

	const navigate = Route.useNavigate();
	const userLanguage = useLocaleStore(store => store.language);

	const updateSearch = (newValues: Partial<z.infer<typeof SearchParamsSchema>>) => {
		navigate({
			search: (prev) => {
				let o = { ...prev };
				for (const key in newValues) {
					if (newValues[key as keyof typeof newValues] === defaultSearchParams[key as keyof typeof defaultSearchParams]) {
						delete o[key as keyof typeof o];
					} else {
						o[key as keyof typeof o] = newValues[key as keyof typeof newValues] as any;
					}
				}
				return o;
			},
		});
	};

	const currentTimeRoundedMinute = Math.floor(Date.now() / (60 * 1000)) * (60 * 1000);

	const listQuery = useEventListQuery({
		search,
		beforeTimestamp: relativity === "past" ? currentTimeRoundedMinute : undefined,
		afterTimestamp: relativity === "future" ? currentTimeRoundedMinute : undefined,
		orderBy: sortBy,
		formatType,
		sourceType,
		error: hasError,
	});

	const queries = useEventQueries(listQuery.data || []);

	const searchOptions = [
		{
			label: "Search",
			control: (
				<Stack gap={0} flex={2} miw={200}>
					<TextInput
						placeholder="Search events..."
						value={search}
						onChange={(event) => updateSearch({ search: event.currentTarget.value })}
						flex={1}
					/>
				</Stack>
			),
		},
		{
			label: "Time",
			control: (
				<Paper withBorder>
					<SegmentedControl<"past" | "all" | "future">
						id="filter-relativity"
						data={[
							{ label: "Past", value: "past" },
							{ label: "All", value: "all" },
							{ label: "Future", value: "future" },
						]}
						value={relativity}
						onChange={(value) => updateSearch({ relativity: value })}
					/>
				</Paper>
			),
		},
		{
			label: "Sort",
			control: (
				<Paper withBorder>
					<SegmentedControl<SortBy>
						data={[
							{ label: "Name", value: "name" },
							{ label: "Date", value: "instanceStart" },
						]}
						value={sortBy}
						onChange={(value) => updateSearch({ sortBy: value })}
					/>
				</Paper>
			),
		},
		{
			label: "Error",
			control: (
				<Paper withBorder>
					<SegmentedControl<"null" | "true" | "false">
						data={[
							{ label: "ERR", value: "true" },
							{ label: "-", value: "null" },
							{ label: "OK", value: "false" },
						]}
						value={hasError === undefined ? "null" : hasError ? "true" : "false"}
						onChange={(value) => updateSearch({ hasError: value === "null" ? undefined : value === "true" })}
					/>
				</Paper>
			),
		},
		{
			label: "Source",
			control: (
				<Autocomplete
					placeholder="Source"
					w={120}
					data={Array.from(EventSourceRegistry.keys())}
					value={sourceType}
					onChange={(value) => updateSearch({ sourceType: value })}
					rightSection={sourceType && <CloseButton onClick={() => updateSearch({ sourceType: "" })} />}
				/>
			),
		},
		{
			label: "Format",
			control: (
				<Autocomplete
					placeholder="Format"
					data={Array.from(EventFormatRegistry.keys())}
					value={formatType}
					onChange={(value) => updateSearch({ formatType: value })}
					rightSection={formatType && <CloseButton onClick={() => updateSearch({ formatType: "" })} />}
				/>
			),
		},
		{
			label: "Reset",
			control: (
				<Tooltip label="Clear search and filters">
					<ActionIcon
						onClick={() => updateSearch(defaultSearchParams)}
						variant="light"
						color="gray"
						size="input-sm"
					>
						<IconFilterOff />
					</ActionIcon>
				</Tooltip>
			),
		},
	];

	const overflowItems = searchOptions.map((option) => (
		<Stack key={option.label} gap={0}>
			<Input.Label>
				{option.label}
			</Input.Label>
			{option.control}
		</Stack>
	));

	const top = "calc(var(--app-shell-header-height, 0px) + var(--app-shell-padding) + var(--safe-area-inset-top))";
	return (
		<Stack>
			<Paper pos="sticky" top={top} p={4} withBorder shadow="md" style={{ zIndex: 5 }}>
				<Stack gap={0}>
					<OverflowList
						gap={4}
						data={overflowItems}
						renderItem={(item) => item}
						renderOverflow={(items) => (
							<Stack flex={1} justify="end" align="end">
								<Popover>
									<Popover.Target>
										<ActionIcon
											size="input-sm"
											variant="light"
											color="gray"
										>
											<Accordion.Chevron />
										</ActionIcon>
									</Popover.Target>
									<Popover.Dropdown>
										<Group gap={4} maw="80vw" wrap="wrap">
											{items}
										</Group>
									</Popover.Dropdown>
								</Popover>
							</Stack>
						)}
					/>
				</Stack>
			</Paper>

			<EventsGrid queries={queries} />
		</Stack >
	)
}

// export const LayersSelect = ({
// 	layers,
// 	onChange,
// 	value,
// }: {
// 	layers: Record<string, Layer>;
// 	value: string[];
// 	onChange: (values: string[]) => void;
// }) => {
// 	const combobox = useCombobox();

// 	const options = Object.keys(layers).map((layerId) => (
// 		<Combobox.Option value={layerId} key={layerId}>
// 			<Group gap={4} wrap="nowrap">
// 				<Checkbox
// 					checked={value.includes(layerId)}
// 					readOnly
// 				/>

// 				{layerId}
// 			</Group>
// 		</Combobox.Option>
// 	));

// 	return (
// 		<Combobox
// 			store={combobox}
// 			onOptionSubmit={(id) => {
// 				onChange(value.includes(id) ? value.filter(v => v !== id) : [...value, id]);
// 			}}
// 			width="max-content"
// 		>
// 			<Combobox.Target>
// 				<Indicator
// 					label={value.length}
// 					size={16}
// 					color="gray.7"
// 					disabled={value.length <= 1}
// 					offset={4}
// 				>
// 					<InputBase
// 						component="button"
// 						type="button"
// 						pointer
// 						rightSection={<Combobox.Chevron />}
// 						rightSectionPointerEvents="none"
// 						onClick={() => combobox.toggleDropdown()}
// 					>
// 						{value.length === 1 ? value[0] : "Layers..."}
// 					</InputBase>
// 				</Indicator>
// 			</Combobox.Target>
// 			<Combobox.Dropdown>
// 				<Combobox.Options>
// 					{options}
// 				</Combobox.Options>
// 			</Combobox.Dropdown>
// 		</Combobox>
// 	);
// };
