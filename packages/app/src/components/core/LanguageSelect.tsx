import { IconArrowUp, IconLanguage } from "@tabler/icons-react-native";
import { DATA_LANGUAGES } from "@vantage/intl";
import { Box } from "../base/Box";
import { Colors } from "../../theme/colors";
import { Text } from "../base/Text";
import { InputWrapper, type InputWrapperProps } from "../base/input/InputWrapper";
import {
	Combobox,
	ComboboxTrigger,
	ComboboxSheet,
	ComboboxList,
	ComboboxSheetSearch,
} from "../base/combobox";
import { FontSize, IconSize, Radius } from "../../theme/sizing";
import { Button } from "../base/button/Button";
import { memo, useCallback, useMemo } from "react";

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

export interface LanguageSelectProps extends Pick<
	InputWrapperProps,
	"label" | "description" | "error" | "required"
> {
	value: string;
	onChange: (value: string) => void;
}

export const LanguageSelect = memo(
	({ label, description, error, required, value, onChange }: LanguageSelectProps) => {
		const isSelected = useCallback((item: string) => item === value, [value]);

		return (
			<InputWrapper label={label} description={description} error={error} required={required}>
				<Combobox onOptionSubmit={onChange}>
					<Box>
						<ComboboxTrigger py="sm" px="sm" gap="sm">
							<Box p="xs" radius={Radius.Default} bg={Colors.PrimaryLight + "33"}>
								<IconLanguage size={IconSize.md} color={Colors.Primary} />
							</Box>
							<LanguageItem value={value} selected={true} />
						</ComboboxTrigger>

						{value !== "en" && (
							<Button
								onPress={() => onChange("en")}
								mt="sm"
								justify="flex-start"
								leftSection={<IconArrowUp size={IconSize.sm} color={Colors.TextDimmed} />}
							>
								Use English
							</Button>
						)}
					</Box>
					<ComboboxSheet header={ComboboxSheetSearch}>
						<ComboboxList
							data={DATA_LANGUAGES}
							filter={filter}
							renderItem={LanguageItem}
							isSelected={isSelected}
							closeOnSelect
						/>
					</ComboboxSheet>
				</Combobox>
			</InputWrapper>
		);
	},
);

export const LanguageItem = memo(({ value }: { value: string; selected: boolean }) => {
	const autonym = useMemo(() => getAutonym(value), [value]);
	const englishName = useMemo(() => getEnglishName(value), [value]);

	return (
		<Box direction="row" flex={1}>
			<Box flex={1}>
				<Text fz={FontSize.sm}>{autonym}</Text>
				<Text fz={FontSize.xs} c={Colors.TextDimmed}>
					{englishName}
				</Text>
			</Box>
		</Box>
	);
});
