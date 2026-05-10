import { CodeBlockModal } from "./CodeBlockModal";
import { ImportJSONModal } from "./ImportJSONModal";
import { ImportURLModal } from "./ImportURLModal";

export const contextModals: Record<string, React.FC<any>> = {
	ImportURLModal,
	ImportJSONModal,
	CodeBlockModal,
} as const;

declare module '@mantine/modals' {
	export interface MantineModalsOverride {
		modals: typeof contextModals;
	}
}
