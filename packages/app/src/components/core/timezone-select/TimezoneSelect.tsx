import { Combobox, ComboboxSheet, ComboboxSearch } from "../../base/combobox";
import { TimezoneSelectTrigger } from "./TimezoneSelectTrigger";
import { TimezoneSelectSheet } from "./TimezoneSelectSheet";
import { InputWrapper, type InputWrapperProps } from "../../base/input/InputWrapper";

export interface TimezoneSelectProps extends Pick<
	InputWrapperProps,
	"label" | "description" | "error" | "required"
> {
	value: string;
	onChange: (value: string) => void;
	variant?: "settings" | "form";
}

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

		<ComboboxSheet>
			{/* <TimezoneSelectSheet /> */}
		</ComboboxSheet>
	</Combobox>
);
