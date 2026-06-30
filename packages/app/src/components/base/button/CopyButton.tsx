import { useRef, useState } from "react";
import * as Clipboard from "expo-clipboard";

export interface CopyButtonProps {
	value: string | (() => string | Promise<string>);
	children: (state: {
		copied: boolean;
		loading: boolean;
		onPress: () => void;
	}) => React.ReactNode;
};

export const CopyButton = (props: CopyButtonProps) => {
	const blockRef = useRef(false);
	const [loading, setLoading] = useState(false);
	const [copied, setCopied] = useState(false);

	const handlePress = async () => {
		if (blockRef.current) return;
		blockRef.current = true;
		setLoading(true);
		const value = typeof props.value === "function" ? await props.value() : props.value;
		await Clipboard.setStringAsync(value);
		setCopied(true);
		setLoading(false);
		setTimeout(() => {
			setCopied(false);
			blockRef.current = false;
		}, 2000);
	};

	return props.children({ copied, loading, onPress: handlePress });
};
