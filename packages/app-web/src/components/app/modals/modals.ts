import { CodeBlockModal } from "./CodeBlockModal";
import { EditEventTagsModal } from "./EditEventTagsModal";
import { EditTagModal } from "./EditTagModal";
import { ManageTagsModal } from "./ManageTagsModal";
import { ImportAtModal } from "./ImportAtModal";
import { ImportJSONModal } from "./ImportJSONModal";
import { ImportURLModal } from "./ImportURLModal";
import { ImportWebDAVModal } from "./ImportWebDAVModal";
import { ImportWikiModal } from "./ImportWikiModal";
import { QRCodeModal } from "./QRCodeModal";

export const contextModals: Record<string, React.FC<any>> = {
	ImportURLModal,
	ImportJSONModal,
	CodeBlockModal,
	QRCodeModal,
	ImportAtModal,
	ImportWikiModal,
	ImportWebDAVModal,
	EditEventTagsModal,
	EditTagModal,
	ManageTagsModal,
} as const;

declare module '@mantine/modals' {
	export interface MantineModalsOverride {
		modals: typeof contextModals;
	}
}
