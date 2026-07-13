import { useEffect, useRef } from "react";

/** Pushes history state on open, pops on close, listens for popstate. */
export const useHistoryBack = (open: boolean, onClose: () => void) => {
	// const didPush = useRef(false);

	// useEffect(() => {
	// 	if (open && !didPush.current) {
	// 		if (typeof window !== "undefined" && window.history?.pushState) {
	// 			window.history.pushState(null, "");
	// 			didPush.current = true;
	// 		}
	// 	} else if (!open && didPush.current) {
	// 		if (typeof window !== "undefined") {
	// 			window.history.replaceState(null, "");
	// 		}
	// 		didPush.current = false;
	// 	}
	// }, [open]);

	// useEffect(() => {
	// 	if (!open || typeof window === "undefined") return;

	// 	const handlePopState = () => {
	// 		didPush.current = false;
	// 		onClose();
	// 	};

	// 	window.addEventListener("popstate", handlePopState);
	// 	return () => window.removeEventListener("popstate", handlePopState);
	// }, [open, onClose]);
};
