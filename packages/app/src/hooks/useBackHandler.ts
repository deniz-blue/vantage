import { useEffect, useRef } from "react";
import { BackHandler } from "react-native";

export const useBackHandler = (open: boolean, onClose: () => void) => {
	const onCloseRef = useRef(onClose);
	onCloseRef.current = onClose;

	useEffect(() => {
		console.log("useBackHandler effect, open:", open);
		if (!open) return;
		const sub = BackHandler.addEventListener("hardwareBackPress", () => {
			console.log("Back button pressed, closing sheet");
			onCloseRef.current();
			return true;
		});
		return () => sub.remove();
	}, [open]);

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
