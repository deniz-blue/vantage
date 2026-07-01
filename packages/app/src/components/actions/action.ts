import { ReactNode } from "react";
import { CopyButtonProps } from "../base/button/CopyButton";

export interface BaseAction {
	label: ReactNode;
	icon?: ReactNode;
	search?: string;
};

export interface CopyAction extends BaseAction {
	type: "copy";
	value: CopyButtonProps["value"];
};

export interface FnAction extends BaseAction {
	type: "fn";
	onRun: () => void;
};

export type Action = CopyAction | FnAction;
