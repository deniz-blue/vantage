import { BottomSheetProvider } from "@swmansion/react-native-bottom-sheet";

export const SheetProvider = ({ children }: { children: React.ReactNode }) => {
	return <BottomSheetProvider>{children}</BottomSheetProvider>;
};
