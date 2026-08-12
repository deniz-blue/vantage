import { Combobox, ComboboxSheet, ComboboxList, ComboboxSheetSearch } from "../../base/combobox";
import { TimezoneSelectTrigger } from "./TimezoneSelectTrigger";
import { InputWrapper, type InputWrapperProps } from "../../base/input/InputWrapper";
import { TimezoneItem } from "./TimezoneItem";
import { useCallback } from "react";

export interface TimezoneSelectProps extends Pick<
	InputWrapperProps,
	"label" | "description" | "error" | "required"
> {
	value: string;
	onChange: (value: string) => void;
	variant?: "settings" | "form";
}

const tz = Intl.supportedValuesOf("timeZone").sort((a, b) => a.localeCompare(b));

export const TimezoneSelect = ({
	label,
	description,
	error,
	required,
	value,
	onChange,
	variant = "settings",
}: TimezoneSelectProps) => {
	const isSelected = useCallback((item: string) => item === value, [value]);

	return (
		<Combobox onOptionSubmit={onChange}>
			<InputWrapper label={label} description={description} error={error} required={required}>
				<TimezoneSelectTrigger value={value} variant={variant} />
			</InputWrapper>

			<ComboboxSheet header={ComboboxSheetSearch}>
				<ComboboxList data={tz} renderItem={TimezoneItem} isSelected={isSelected} closeOnSelect />
			</ComboboxSheet>
		</Combobox>
	);
};
