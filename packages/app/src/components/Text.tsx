import { TextProps as RNTextProps, Text as RNText } from "react-native";
import { Colors } from "../theme/colors";

export interface TextProps extends RNTextProps {

};

export const Text = (props: TextProps) => {
	const style: TextProps["style"] = {
		color: Colors.Text,
	};

	return (
		<RNText
			style={[style, props.style]}
			{...props}
		/>
	);
};
