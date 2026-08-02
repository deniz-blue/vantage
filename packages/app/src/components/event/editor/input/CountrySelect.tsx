import { DATA_COUNTRIES, UtilCountryCode } from "@vantage/intl";
import { Select, SelectItemProps } from "../../../base/input/Select";
import { Text } from "../../../base/Text";
import { Box } from "../../../base/Box";
import { useLocaleStore } from "../../../../stores/useLocaleStore";

export const CountrySelect = ({
	value,
	onChange,
}: {
	value: string | undefined;
	onChange: (value: string | undefined) => void;
}) => {
	return (
		<Select
			label="Country"
			data={DATA_COUNTRIES}
			value={value}
			onChange={onChange}
			renderItem={CountryItem}
			getSearchText={(item) =>
				item ? UtilCountryCode.getLabel(item, useLocaleStore.getState().language) : ""
			}
		/>
	);
};

export const CountryItem = ({ value, trigger }: SelectItemProps<string | undefined>) => {
	const userLanguage = useLocaleStore((s) => s.language);
	if (!value) return null;
	if (trigger) return <Text>{UtilCountryCode.toEmoji(value)}</Text>;
	return (
		<Box direction="row" gap="sm" align="center">
			<Text>{UtilCountryCode.toEmoji(value)}</Text>
			<Text>{UtilCountryCode.getLabel(value, userLanguage)}</Text>
		</Box>
	);
};
