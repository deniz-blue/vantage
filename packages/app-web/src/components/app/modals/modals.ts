import { CodeBlockModal } from "./CodeBlockModal";
import { ImportJSONModal } from "./ImportJSONModal";
import { ImportURLModal } from "./ImportURLModal";
import { QRCodeModal } from "./QRCodeModal";

export const contextModals: Record<string, React.FC<any>> = {
	ImportURLModal,
	ImportJSONModal,
	CodeBlockModal,
	QRCodeModal,
} as const;

declare module '@mantine/modals' {
	export interface MantineModalsOverride {
		modals: typeof contextModals;
	}
}
