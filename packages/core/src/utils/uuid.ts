import { v7 } from "uuid";

export type UUID = `${string}-${string}-${string}-${string}-${string}`;
export const randomUUID = (): UUID => {
	return v7() as UUID;
};
