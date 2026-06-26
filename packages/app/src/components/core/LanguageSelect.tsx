import { IconLanguage } from "@tabler/icons-react-native";
import { LANGUAGES } from "@vantage/intl";
import { Box } from "../base/Box";
import { Colors } from "../../theme/colors";
import { Text } from "../base/Text";
import { ActionIcon } from "../base/ActionIcon";
import { InputWrapper, type InputWrapperProps } from "../base/InputWrapper";
import {
	Combobox,
	ComboboxTrigger,
	ComboboxSheet,
	ComboboxSearch,
	ComboboxList,
} from "../base/combobox";
import { FontSize, IconSize } from "../../theme/sizing";

const getAutonym = (code: string): string =>
	new Intl.DisplayNames([code], { type: "language" }).of(code) || code;

const getEnglishName = (code: string): string =>
	new Intl.DisplayNames("en", { type: "language" }).of(code) || code;

const filter = (code: string, search: string): boolean => {
	const q = search.toLowerCase();
	return (
		getAutonym(code).toLowerCase().includes(q) ||
		getEnglishName(code).toLowerCase().includes(q) ||
		code.includes(q)
	);
};

const renderLanguageItem = (code: string, selected: boolean) => (
	<>
		<Box miw={36} h={36} radius={8} bg={selected ? Colors.Primary : Colors.BackgroundLight} align="center" justify="center" mr="md" px={8}>
			<Text fz={12} fw="700" c={selected ? "#fff" : Colors.TextDimmed}>
				{code.toUpperCase()}
			</Text>
		</Box>
		<Box flex={1}>
			<Text fz={15} fw={selected ? "600" : "400"}>
				{getAutonym(code)}
			</Text>
			<Box mt={1}>
				<Text fz={12} c={Colors.TextDimmed}>
					{getEnglishName(code)}
				</Text>
			</Box>
		</Box>
	</>
);

export interface LanguageSelectProps
	extends Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
	value: string;
	onChange: (value: string) => void;
}

export const LanguageSelect = ({
	label,
	description,
	error,
	required,
	value,
	onChange,
}: LanguageSelectProps) => {
	return (
		<InputWrapper
			label={label}
			description={description}
			error={error}
			required={required}
		>
			<Combobox value={value} onChange={onChange}>
				<ComboboxTrigger>
					<ActionIcon bg={Colors.PrimaryLight + "33"}>
						<IconLanguage size={IconSize.md} color={Colors.Primary} />
					</ActionIcon>
					<Box flex={1}>
						<Text fz={FontSize.sm} fw="600">
							{getAutonym(value)}
						</Text>
						<Box mt={1}>
							<Text fz={FontSize.xs} c={Colors.TextDimmed}>
								{getEnglishName(value)}
							</Text>
						</Box>
					</Box>
				</ComboboxTrigger>
				<ComboboxSheet
					search={<ComboboxSearch placeholder="Search languages…" />}
				>
					<ComboboxList
						data={LANGUAGES}
						filter={filter}
						renderItem={renderLanguageItem}
					/>
				</ComboboxSheet>
			</Combobox>
		</InputWrapper>
	);
};
