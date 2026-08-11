import { Colors } from "../../../theme/colors";
import { FontSize } from "../../../theme/sizing";
import { ButtonBase, ButtonBaseProps } from "../ButtonBase";
import { Text, TextProps } from "../Text";

export interface InlineTextButtonProps
	extends
		Omit<TextProps, "onPress" | "onLongPress">,
		Pick<ButtonBaseProps, "onPress" | "onLongPress"> {}

export const InlineTextButton = ({ onPress, onLongPress, ...props }: InlineTextButtonProps) => {
	return (
		<ButtonBase onPress={onPress} onLongPress={onLongPress}>
			<Text c={Colors.Blue} fz={FontSize.sm} {...props} />
		</ButtonBase>
	);
};
