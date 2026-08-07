import type { ReactNode } from "react";
import type { Resolvable } from "../base/button/AsyncButton";

export interface BaseAction {
	label: ReactNode;
	icon?: ReactNode;
	search?: string;
}

export interface ShareData {
	content: string;
	title?: string;
}

export interface ActionMap {
	copy: {
		value: Resolvable<string>;
	};
	share: {
		value: Resolvable<string>;
	};
	fn: {
		onRun: () => Promise<void> | void;
		danger?: boolean;
	};
	raw: {
		value: string;
	};
	link: {
		url: string;
	};
}

export type Action = {
	[Ty in keyof ActionMap]: BaseAction & { type: Ty } & ActionMap[Ty];
}[keyof ActionMap];
