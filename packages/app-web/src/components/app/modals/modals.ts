import { CodeBlockModal } from "./CodeBlockModal";
import { ImportAtModal } from "./ImportAtModal";
import { ImportJSONModal } from "./ImportJSONModal";
import { ImportURLModal } from "./ImportURLModal";
import { QRCodeModal } from "./QRCodeModal";

export const contextModals: Record<string, React.FC<any>> = {
	ImportURLModal,
	ImportJSONModal,
	CodeBlockModal,
	QRCodeModal,
	ImportAtModal,
} as const;

declare module '@mantine/modals' {
	export interface MantineModalsOverride {
		modals: typeof contextModals;
	}
}
