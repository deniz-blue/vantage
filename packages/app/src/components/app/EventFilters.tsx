import { createContext } from "react";
import { Box } from "../base/Box";
import { TextInput } from "../base/input/TextInput";
import { ListOptions } from "@vantage/core";
import { Draft } from "immer";

export const EventFiltersContext = createContext<
	[ListOptions, (recipe: (draft: Draft<ListOptions>) => void) => void]
>([{}, () => {}]);

export const EventFilters = () => {};

export const EventFiltersSheetContent = () => {
	return (
		<Box gap="md">
			<TextInput />
		</Box>
	);
};
