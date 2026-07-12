import { useRef, useState } from "react";

export interface AsyncButtonProps {
	fn: () => Promise<void>;
	cooldown?: number;
	children: (state: { loading: boolean; onPress: () => void; blocked: boolean }) => React.ReactNode;
}

export const AsyncButton = ({ fn, cooldown, children }: AsyncButtonProps) => {
	const blockRef = useRef(false);
	const [loading, setLoading] = useState(false);
	const [blocked, setBlocked] = useState(false);

	const onPress = async () => {
		if (blockRef.current) return;
		blockRef.current = true;
		setLoading(true);
		setBlocked(true);
		try {
			await fn();
		} finally {
			setLoading(false);
			if (cooldown) {
				setTimeout(() => {
					setBlocked(false);
					blockRef.current = false;
				}, cooldown);
			} else {
				setBlocked(false);
				blockRef.current = false;
			}
		}
	};

	return children({ loading, onPress, blocked });
};
