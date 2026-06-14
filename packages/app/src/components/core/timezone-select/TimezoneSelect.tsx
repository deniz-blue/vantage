import { useState } from "react";
import { TimezoneSelectTrigger } from "./TimezoneSelectTrigger";
import { TimezoneSelectSheet } from "./TimezoneSelectSheet";
import { InputWrapper, InputWrapperProps } from "../../base/InputWrapper";
import { Sheet } from "../../base/Sheet";

export interface TimezoneSelectProps
	extends Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
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
}: TimezoneSelectProps) => {
	const [open, setOpen] = useState(false);
	const [region, setRegion] = useState<string | null>(null);

	const handleOpen = () => {
		setRegion(null);
		setOpen(true);
	};

	const handleSelect = (tz: string) => {
		onChange(tz);
		setOpen(false);
	};

	const handleSelectRegion = (r: string) => {
		setRegion(r);
	};

	return (
		<>
			<InputWrapper
				label={label}
				description={description}
				error={error}
				required={required}
			>
				<TimezoneSelectTrigger
					value={value}
					variant={variant}
					onOpen={handleOpen}
					onChange={onChange}
				/>
			</InputWrapper>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<TimezoneSelectSheet
					value={value}
					region={region}
					onSelect={handleSelect}
					onSelectRegion={handleSelectRegion}
				/>
			</Sheet>
		</>
	);
};
