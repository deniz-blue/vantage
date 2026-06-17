import { createContext, useContext } from "react";
import { Editor } from "./useEditor";
import { OpenEvnt } from "@evnt/types";

export const EventFormContext = createContext<{
	editor: Editor<OpenEvnt>;
}>({
	editor: null as any,
});

export const useEventFormContext = () => {
	const ctx = useContext(EventFormContext);
	if (!ctx) throw new Error("useEventFormContext must be used within an EventFormContext.Provider");
	return ctx;
};
