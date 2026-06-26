import { useCallback, type ReactNode } from "react";
import { Combobox, ComboboxTrigger, ComboboxSheet, ComboboxSearch, ComboboxList } from "./combobox";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { Box } from "./Box";
import { Text } from "./Text";
import { FontSize } from "../../theme/sizing";

export interface SelectProps<T> extends Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
	data: readonly T[];
	value: T;
	onChange: (value: T) => void;
	renderItem: (item: T) => ReactNode;
	getSearchText?: (item: T) => string;
	placeholder?: string;
	searchable?: boolean;
}

export const Select = <T,>({
	data,
	value,
	onChange,
	renderItem,
	getSearchText,
	placeholder = "Select…",
	searchable = false,
	label,
	description,
	error,
	required,
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
						{renderItem(value) || placeholder}
					</Text>
				</ComboboxTrigger>
				<ComboboxSheet
					search={searchable ? <ComboboxSearch /> : undefined}
				>
					<ComboboxList
						data={data}
						filter={filter}
						renderItem={(item, _selected) => (
							<Box flex={1}>
								<Text fz={FontSize.sm}>{renderItem(item)}</Text>
							</Box>
						)}
					/>
				</ComboboxSheet>
			</Combobox>
		</InputWrapper>
	);
};
