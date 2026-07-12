import { Button, ButtonProps } from "../base/button/Button";
import { CopyButton, CopyButtonProps } from "../base/button/CopyButton";

export interface AppCopyButtonProps
	extends Pick<CopyButtonProps, "value" | "duration">, ButtonProps {}

export const AppCopyButton = ({ value, duration, ...props }: AppCopyButtonProps) => {
	return (
		<CopyButton value={value} duration={duration}>
			{({ copied, loading, onPress }) => (
				<Button
					{...props}
					onPress={() => {
						onPress();
						props.onPress?.();
					}}
					{...(copied
						? {
								children: "Copied!",
							}
						: {})}
					{...(loading
						? {
								loading: true,
							}
						: {})}
				/>
			)}
		</CopyButton>
	);
};
