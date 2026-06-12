import { TextInput, TouchableOpacity, FlatList } from "react-native";
import {
	createContext,
	useCallback,
	useContext,
	useEffect,
	useMemo,
	useRef,
	useState,
	type ReactNode,
} from "react";
import { IconChevronRight, IconSearch, IconCheck } from "@tabler/icons-react-native";
import { Box } from "./Box";
import { Text } from "./Text";
import { Button } from "./Button";
import { Sheet } from "./Sheet";
import { Colors } from "../../theme/colors";

// === Context ===

export interface ComboboxContext {
	value: string;
	onChange: (value: string) => void;
	search: string;
	setSearch: (value: string) => void;
	opened: boolean;
	open: () => void;
	close: () => void;
}

const Ctx = createContext<ComboboxContext | null>(null);

export const useComboboxCtx = (): ComboboxContext => {
	const ctx = useContext(Ctx);
	if (!ctx) throw new Error("useComboboxCtx must be inside <Combobox>");
	return ctx;
};

// === Root — pure context provider ===

export interface ComboboxProps {
	children: ReactNode;
	value: string;
	onChange: (value: string) => void;
}

export const Combobox = ({ children, value, onChange }: ComboboxProps) => {
	const [opened, setOpened] = useState(false);
	const [search, setSearch] = useState("");

	const open = useCallback(() => setOpened(true), []);
	const close = useCallback(() => {
		setOpened(false);
		setSearch("");
	}, []);

	const ctx = useMemo<ComboboxContext>(
		() => ({ value, onChange, search, setSearch, opened, open, close }),
		[value, onChange, search, opened, close],
	);

	return <Ctx.Provider value={ctx}>{children}</Ctx.Provider>;
};

// === Trigger — renders the pressable button ===

export interface ComboboxTriggerProps {
	children: ReactNode;
}

export const ComboboxTrigger = ({ children }: ComboboxTriggerProps) => {
	const ctx = useComboboxCtx();
	return (
		<Button
			variant="default"
			rightSection={<IconChevronRight size={18} color={Colors.TextDimmed} />}
			onPress={ctx.open}
			style={{ borderRadius: 12 }}
		>
			<Box flex={1} direction="row" align="center">{children}</Box>
		</Button>
	);
};

// === Sheet — wraps content in the bottom sheet ===

export interface ComboboxSheetProps {
	children: ReactNode;
	height?: number;
}

export const ComboboxSheet = ({ children, height = 0.7 }: ComboboxSheetProps) => {
	const ctx = useComboboxCtx();
	return (
		<Sheet open={ctx.opened} onClose={ctx.close} height={height}>
			{children}
		</Sheet>
	);
};

// === Search — built-in search bar ===

export interface ComboboxSearchProps {
	placeholder?: string;
}

export const ComboboxSearch = ({ placeholder = "Search…" }: ComboboxSearchProps) => {
	const ctx = useComboboxCtx();
	return (
		<Box p="md" style={{ borderBottomWidth: 1, borderBottomColor: Colors.BackgroundLight }}>
			<Box direction="row" align="center" bg={Colors.BackgroundLight} radius={10} px={12} gap={8}>
				<IconSearch size={18} color={Colors.TextDimmed} />
				<TextInput
					value={ctx.search}
					onChangeText={ctx.setSearch}
					placeholder={placeholder}
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
	);
};

// === List — FlatList with selection boilerplate ===

export interface ComboboxListProps<T> {
	data: readonly T[];
	renderItem: (item: T, selected: boolean) => ReactNode;
	filter?: (item: T, search: string) => boolean;
}

export const ComboboxList = <T,>({
	data,
	renderItem,
	filter,
}: ComboboxListProps<T>) => {
	const ctx = useComboboxCtx();
	const ref = useRef<FlatList<T>>(null);

	const items = useMemo(() => {
		if (!ctx.search.trim() || !filter) return data;
		return data.filter((item) => filter(item, ctx.search));
	}, [data, ctx.search, filter]);

	useEffect(() => {
		const idx = items.findIndex((item) => item === (ctx.value as unknown));
		if (idx < 0) return;
		const timer = setTimeout(() => {
			ref.current?.scrollToIndex({ index: idx, animated: true, viewPosition: 0.3 });
		}, 350);
		return () => clearTimeout(timer);
	}, [items, ctx.value]);

	const onScrollToIndexFailed = useCallback(
		(info: { index: number; averageItemLength: number }) => {
			ref.current?.scrollToOffset({
				offset: info.index * info.averageItemLength,
				animated: true,
			});
		},
		[],
	);

	return (
		<FlatList
			ref={ref}
			data={items as any}
			keyExtractor={(_item: any, i: number) => String(i)}
			style={{ flex: 1 }}
			keyboardShouldPersistTaps="handled"
			onScrollToIndexFailed={onScrollToIndexFailed}
			contentContainerStyle={{ paddingVertical: 4 }}
			renderItem={({ item }: { item: T }) => {
				const selected = item === (ctx.value as unknown);
				return (
					<TouchableOpacity
						onPress={() => { ctx.onChange(item as any); ctx.close(); }}
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
						{renderItem(item, selected)}
						{selected && <IconCheck size={20} color={Colors.Primary} />}
					</TouchableOpacity>
				);
			}}
		/>
	);
};

// === Convenience: Icon box for trigger ===

export interface ComboboxIconProps {
	children: ReactNode;
}

export const ComboboxIcon = ({ children }: ComboboxIconProps) => (
	<Box w={40} h={40} radius={10} bg={Colors.PrimaryLight + "33"} align="center" justify="center" mr="md">
		{children}
	</Box>
);

// === Convenience: Badge for list items ===

export interface ComboboxBadgeProps {
	children: ReactNode;
	selected: boolean;
}

export const ComboboxBadge = ({ children, selected }: ComboboxBadgeProps) => (
	<Box miw={36} h={36} radius={8} bg={selected ? Colors.Primary : Colors.BackgroundLight} align="center" justify="center" mr="md" px={8}>
		{typeof children === "string" ? (
			<Text fz={12} fw="700" c={selected ? "#fff" : Colors.TextDimmed}>
				{children}
			</Text>
		) : (
			children
		)}
	</Box>
);
