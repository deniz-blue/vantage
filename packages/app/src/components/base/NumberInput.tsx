import { useCallback, type ReactNode } from "react";
import { TextInput, type TextInputProps } from "./TextInput";

export interface NumberInputProps
	extends Omit<
		TextInputProps,
		"value" | "onChangeText" | "onChange" | "keyboardType"
	> {
	value?: number;
	onChange?: (value: number | undefined) => void;
	leftSection?: ReactNode;
	rightSection?: ReactNode;
}

export const NumberInput = ({
	value,
	onChange,
	...rest
}: NumberInputProps) => {
	const handleChangeText = useCallback(
		(text: string) => {
			if (text === "") {
				onChange?.(undefined);
				return;
			}
			onChange?.(Number(text));
		},
		[onChange],
	);

	return (
		<TextInput
			value={value !== undefined ? String(value) : ""}
			onChangeText={handleChangeText}
			keyboardType="decimal-pad"
			{...rest}
		/>
	);
};
