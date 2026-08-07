import * as Clipboard from "expo-clipboard";
import { AsyncButton, Resolvable } from "./AsyncButton";

export interface CopyButtonProps {
	value: Resolvable<string>;
	duration?: number;
	children: (state: { copied: boolean; loading: boolean; onPress: () => void }) => React.ReactNode;
}

export const CopyButton = ({ value, duration = 2000, children }: CopyButtonProps) => {
	const copy = async () => {
		const text = typeof value === "function" ? await value() : value;
		await Clipboard.setStringAsync(text);
	};

	return (
		<AsyncButton fn={copy} cooldown={duration}>
			{({ blocked, loading, onPress }) =>
				children({
					onPress,
					loading,
					copied: !loading && blocked,
				})
			}
		</AsyncButton>
	);
};
