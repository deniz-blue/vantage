import { ReactNode } from "react";
import { CopyButtonProps } from "../base/button/CopyButton";

export interface BaseAction {
	label: ReactNode;
	icon?: ReactNode;
	search?: string;
}

export interface ActionMap {
	copy: {
		value: CopyButtonProps["value"];
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
