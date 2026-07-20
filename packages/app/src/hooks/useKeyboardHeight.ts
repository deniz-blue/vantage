import { useEffect } from "react";
import { Keyboard } from "react-native";
import { useSharedValue } from "react-native-reanimated";

export const useKeyboardHeight = (disabled: boolean = false) => {
	const keyboardHeight = useSharedValue(0);

	useEffect(() => {
		if (disabled) return;
		const show = Keyboard.addListener("keyboardDidShow", (e) => {
			keyboardHeight.value = e.endCoordinates.height;
		});
		const hide = Keyboard.addListener("keyboardDidHide", (e) => {
			keyboardHeight.value = e.endCoordinates.height;
		});
		return () => {
			show.remove();
			hide.remove();
		};
	}, [disabled]);

	return keyboardHeight;
};
