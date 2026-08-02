import { ComponentType, useCallback } from "react";
import {
	Combobox,
	ComboboxTrigger,
	ComboboxSheet,
	ComboboxSheetList,
	ComboboxSheetSearch,
} from "../combobox";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { Text } from "../Text";
import { FontSize } from "../../../theme/sizing";

export interface SelectItemProps<T> {
	value: T;
	selected: boolean;
	trigger?: boolean;
}

export interface SelectProps<T> extends Pick<
	InputWrapperProps,
	"label" | "description" | "error" | "required"
> {
	data: readonly T[];
	value: T;
	onChange: (value: T) => void;
	renderItem?: ComponentType<SelectItemProps<T>>;
	getSearchText?: (item: T) => string;
	placeholder?: string;
	searchable?: boolean;
}

const DefaultItemComponent = <T,>({ value }: { value: T }) => (
	<Text fz={FontSize.sm}>{String(value)}</Text>
);

export const Select = <T,>({
	data,
	value,
	onChange,
	renderItem: ItemComponent = DefaultItemComponent,
	getSearchText,
	placeholder = "Select…",
	label,
	description,
	error,
	required,
	searchable = true,
}: SelectProps<T>) => {
	const filter = useCallback(
		(item: T, search: string) => {
			if (!getSearchText) return true;
			return getSearchText(item).toLowerCase().includes(search.toLowerCase());
		},
		[getSearchText],
	);

	return (
		<InputWrapper label={label} description={description} error={error} required={required}>
			<Combobox value={value} onChange={onChange}>
				<ComboboxTrigger>
					<Text fz={FontSize.sm}>
						{value ? <ItemComponent value={value} trigger selected /> : placeholder}
					</Text>
				</ComboboxTrigger>
				<ComboboxSheet
					scrollable={searchable}
					header={searchable ? ComboboxSheetSearch : undefined}
				>
					<ComboboxSheetList
						data={data}
						filter={filter}
						renderItem={ItemComponent || DefaultItemComponent}
						withPadding={searchable}
					/>
				</ComboboxSheet>
			</Combobox>
		</InputWrapper>
	);
};
