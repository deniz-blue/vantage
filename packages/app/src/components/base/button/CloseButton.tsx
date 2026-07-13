import { Box, BoxProps } from "../Box";
import { IconX } from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { IconSize } from "../../../theme/sizing";
import { ButtonBase } from "../ButtonBase";

export interface CloseButtonProps extends Omit<BoxProps, "children"> {
	onPress?: () => void;
}

export const CloseButton = ({ onPress, ...rest }: CloseButtonProps) => {
	return (
		<Box
			component={ButtonBase}
			align="center"
			justify="center"
			activeOpacity={0.7}
			onPress={onPress}
			{...(rest as any)}
		>
			<IconX size={IconSize.xs} color={Colors.TextDimmed} />
		</Box>
	);
};
