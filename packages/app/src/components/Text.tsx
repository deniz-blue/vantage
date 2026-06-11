import { TextProps as RNTextProps, Text as RNText } from "react-native";
import { Colors } from "../theme/colors";

export interface TextProps extends RNTextProps {

};

export const Text = (props: TextProps) => {
	const { style: propStyle, ...rest } = props;

	return (
		<RNText
			style={[{ color: Colors.Text }, propStyle]}
			{...rest}
		/>
	);
};
