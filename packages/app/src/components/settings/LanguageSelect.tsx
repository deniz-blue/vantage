import { IconLanguage } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import {
	Combobox,
	ComboboxTrigger,
	ComboboxIcon,
	ComboboxBadge,
	ComboboxSheet,
	ComboboxSearch,
	ComboboxList,
} from "../base/Combobox";
import { Colors } from "../../theme/colors";

const LANGUAGES = [
	"en", "es", "fr", "de", "pt", "ru", "ja", "ko", "zh", "ar",
	"it", "nl", "pl", "sv", "da", "no", "fi", "cs", "hu", "ro",
	"uk", "el", "he", "hi", "th", "vi", "tr", "id", "ms", "bn",
	"sw", "tl", "ne", "si", "my", "km", "lo", "ka", "hy", "az",
] as const;

const autonymDisplay = new Intl.DisplayNames([], { type: "language" });
const englishDisplay = new Intl.DisplayNames("en", { type: "language" });

const getAutonym = (code: string): string =>
	autonymDisplay.of(code) || code;

const getEnglishName = (code: string): string =>
	englishDisplay.of(code) || code;

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
		<ComboboxBadge selected={selected}>
			{code.toUpperCase()}
		</ComboboxBadge>
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

export interface LanguageSelectProps {
	value: string;
	onChange: (value: string) => void;
}

export const LanguageSelect = ({ value, onChange }: LanguageSelectProps) => {
	return (
		<Box>
			<Text fz={13} c={Colors.TextDimmed} fw="600" mb={8} ml={2}>
				Language
			</Text>
			<Combobox value={value} onChange={onChange}>
				<ComboboxTrigger>
					<ComboboxIcon>
						<IconLanguage size={20} color={Colors.Primary} />
					</ComboboxIcon>
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
		</Box>
	);
};
