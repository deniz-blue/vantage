import { ComponentType, useCallback } from "react";
import {
	Combobox,
	ComboboxTrigger,
	ComboboxSheet,
	ComboboxList,
	ComboboxSheetSearch,
} from "../combobox";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { Text } from "../Text";
import { FontSize } from "../../../theme/sizing";
import { SelectItemProps } from "./Select";

export interface MultiSelectProps<T> extends Pick<
	InputWrapperProps,
	"label" | "description" | "error" | "required"
> {
	data: readonly T[];
	value: T[];
	buttonContent: React.ReactNode;
	onChange: (value: T[]) => void;
	renderItem?: ComponentType<SelectItemProps<T>>;
	isSelected?: (item: T) => boolean;
	getSearchText?: (item: T) => string;
	searchable?: boolean;
	disabled?: boolean;
}

const DefaultItemComponent = <T,>({ value }: { value: T }) => (
	<Text fz={FontSize.sm}>{String(value)}</Text>
);

export const MultiSelect = <T,>({
	data,
	value,
	onChange,
	renderItem: ItemComponent = DefaultItemComponent,
	getSearchText,
	buttonContent,
	isSelected: isSelectedProp,
	label,
	description,
	error,
	required,
	searchable = true,
	disabled,
}: MultiSelectProps<T>) => {
	const isSelected = useCallback(
		(item: T) => {
			if (isSelectedProp) return isSelectedProp(item);
			return value.includes(item);
		},
		[value, isSelectedProp],
	);

	const onOptionSubmit = useCallback(
		(item: T) => {
			if (isSelected(item)) {
				onChange(value.filter((v) => v !== item));
			} else {
				onChange([...value, item]);
			}
		},
		[isSelected, onChange],
	);

	const filter = useCallback(
		(item: T, search: string) => {
			if (!getSearchText) return true;
			return getSearchText(item).toLowerCase().includes(search.toLowerCase());
		},
		[getSearchText],
	);

	return (
		<InputWrapper label={label} description={description} error={error} required={required}>
			<Combobox onOptionSubmit={onOptionSubmit}>
				<ComboboxTrigger disabled={disabled}>{buttonContent}</ComboboxTrigger>
				<ComboboxSheet
					scrollable={searchable}
					header={searchable ? ComboboxSheetSearch : undefined}
				>
					<ComboboxList
						data={data}
						filter={filter}
						renderItem={ItemComponent || DefaultItemComponent}
						isSelected={isSelected}
						withPadding={searchable}
					/>
				</ComboboxSheet>
			</Combobox>
		</InputWrapper>
	);
};
