import { FlatList, TouchableOpacity, TextInput } from "react-native";
import { useCallback, useMemo, useState } from "react";
import { IconLanguage, IconChevronRight, IconCheck, IconSearch } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Sheet } from "../base/Sheet";
import { Colors } from "../../theme/colors";

// Common BCP-47 language codes
const LANGUAGES = [
	"en", "es", "fr", "de", "pt", "ru", "ja", "ko", "zh", "ar",
	"it", "nl", "pl", "sv", "da", "no", "fi", "cs", "hu", "ro",
	"uk", "el", "he", "hi", "th", "vi", "tr", "id", "ms", "bn",
	"sw", "tl", "ne", "si", "my", "km", "lo", "ka", "hy", "az",
] as const;

// Module-level formatters — created once
const autonymDisplay = new Intl.DisplayNames([], { type: "language" });
const englishDisplay = new Intl.DisplayNames("en", { type: "language" });

const getAutonym = (code: string): string =>
	autonymDisplay.of(code) || code;

const getEnglishName = (code: string): string =>
	englishDisplay.of(code) || code;

export interface LanguageSelectProps {
	value: string;
	onChange: (value: string) => void;
}

export const LanguageSelect = ({ value, onChange }: LanguageSelectProps) => {
	const [opened, setOpened] = useState(false);
	const [search, setSearch] = useState("");

	const close = useCallback(() => {
		setOpened(false);
		setSearch("");
	}, []);

	const filtered = useMemo(() => {
		if (!search.trim()) return LANGUAGES;
		const q = search.toLowerCase();
		return LANGUAGES.filter((code) => {
			const label = getAutonym(code).toLowerCase();
			const english = getEnglishName(code).toLowerCase();
			return label.includes(q) || english.includes(q) || code.includes(q);
		});
	}, [search]);

	return (
		<Box>
			<Text style={{ fontSize: 13, color: Colors.TextDimmed, fontWeight: "600", marginBottom: 8, marginLeft: 2 }}>
				Language
			</Text>
			<TouchableOpacity
				onPress={() => setOpened(true)}
				activeOpacity={0.8}
				style={{
					backgroundColor: Colors.BackgroundLight,
					borderRadius: 12,
					padding: 14,
					flexDirection: "row",
					alignItems: "center",
				}}
			>
				<Box
					style={{
						width: 40,
						height: 40,
						borderRadius: 10,
						backgroundColor: Colors.PrimaryLight + "33",
						alignItems: "center",
						justifyContent: "center",
						marginRight: 12,
					}}
				>
					<IconLanguage size={20} color={Colors.Primary} />
				</Box>
				<Box style={{ flex: 1 }}>
					<Text style={{ fontSize: 15, fontWeight: "600" }}>
						{getAutonym(value)}
					</Text>
					<Text style={{ fontSize: 12, color: Colors.TextDimmed, marginTop: 1 }}>
						{getEnglishName(value)}
					</Text>
				</Box>
				<IconChevronRight size={18} color={Colors.TextDimmed} />
			</TouchableOpacity>

			<Sheet open={opened} onClose={close} height={0.7}>
				<Box p="md" style={{ borderBottomWidth: 1, borderBottomColor: Colors.BackgroundLight }}>
					<Text style={{ fontSize: 18, fontWeight: "bold", marginBottom: 10 }}>
						Select Language
					</Text>
					<Box
						style={{
							flexDirection: "row",
							alignItems: "center",
							backgroundColor: Colors.BackgroundLight,
							borderRadius: 10,
							paddingHorizontal: 12,
							gap: 8,
						}}
					>
						<IconSearch size={18} color={Colors.TextDimmed} />
						<TextInput
							value={search}
							onChangeText={setSearch}
							placeholder="Search languages…"
							placeholderTextColor={Colors.TextDimmed}
							style={{
								flex: 1,
								paddingVertical: 10,
								color: Colors.Text,
								fontSize: 15,
							}}
						/>
					</Box>
				</Box>
				<FlatList
					data={filtered}
					keyExtractor={(item) => item}
					style={{ flex: 1 }}
					keyboardShouldPersistTaps="handled"
					contentContainerStyle={{ paddingVertical: 4 }}
					renderItem={({ item }) => {
						const selected = item === value;
						return (
							<TouchableOpacity
								onPress={() => {
									onChange(item);
									close();
								}}
								activeOpacity={0.7}
								style={{
									paddingHorizontal: 16,
									paddingVertical: 14,
									backgroundColor: selected ? Colors.PrimaryLight + "33" : "transparent",
									flexDirection: "row",
									alignItems: "center",
									borderRadius: 10,
									marginHorizontal: 8,
									marginVertical: 2,
								}}
							>
								<Box
									style={{
										width: 36,
										height: 36,
										borderRadius: 8,
										backgroundColor: selected ? Colors.Primary : Colors.BackgroundLight,
										alignItems: "center",
										justifyContent: "center",
										marginRight: 12,
									}}
								>
									<Text style={{ fontSize: 12, fontWeight: "700", color: selected ? "#fff" : Colors.TextDimmed }}>
										{item.toUpperCase()}
									</Text>
								</Box>
								<Box style={{ flex: 1 }}>
									<Text style={{ fontSize: 15, fontWeight: selected ? "600" : "400" }}>
										{getAutonym(item)}
									</Text>
									<Text style={{ fontSize: 12, color: Colors.TextDimmed, marginTop: 1 }}>
										{getEnglishName(item)}
									</Text>
								</Box>
								{selected && (
									<IconCheck size={20} color={Colors.Primary} />
								)}
							</TouchableOpacity>
						);
					}}
				/>
			</Sheet>
		</Box>
	);
};
