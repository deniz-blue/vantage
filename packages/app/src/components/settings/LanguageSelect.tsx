import { IconLanguage } from "@tabler/icons-react-native";
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
} from "../combobox";
const LANGUAGES = [
	"en", "es", "fr", "de", "pt", "ru", "ja", "ko", "zh", "ar",
	"it", "nl", "pl", "sv", "da", "no", "fi", "cs", "hu", "ro",
	"uk", "el", "he", "hi", "th", "vi", "tr", "id", "ms", "bn",
	"sw", "tl", "ne", "si", "my", "km", "lo", "ka", "hy", "az",
] as const;

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
			<Text fz={12} c={Colors.TextDimmed} mt={1}>
				{getEnglishName(code)}
			</Text>
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
					<ActionIcon style={{ backgroundColor: Colors.PrimaryLight + "33", width: 40, height: 40, borderRadius: 10 }}>
						<IconLanguage size={20} color={Colors.Primary} />
					</ActionIcon>
					<Box flex={1}>
						<Text fz={15} fw="600">
							{getAutonym(value)}
						</Text>
						<Text fz={12} c={Colors.TextDimmed} mt={1}>
							{getEnglishName(value)}
						</Text>
					</Box>
				</ComboboxTrigger>
				<ComboboxSheet>
					<ComboboxSearch placeholder="Search languages…" />
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
