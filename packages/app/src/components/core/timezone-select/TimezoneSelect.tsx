import {
	Combobox,
	ComboboxSheet,
	ComboboxSheetList,
	ComboboxSheetSearch,
} from "../../base/combobox";
import { TimezoneSelectTrigger } from "./TimezoneSelectTrigger";
import { InputWrapper, type InputWrapperProps } from "../../base/input/InputWrapper";
import { TimezoneItem } from "./TimezoneItem";

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
}: TimezoneSelectProps) => (
	<Combobox value={value} onChange={onChange}>
		<InputWrapper label={label} description={description} error={error} required={required}>
			<TimezoneSelectTrigger variant={variant} />
		</InputWrapper>

		<ComboboxSheet header={ComboboxSheetSearch}>
			<ComboboxSheetList data={tz} renderItem={TimezoneItem} />
		</ComboboxSheet>
	</Combobox>
);
