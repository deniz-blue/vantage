import { View, ViewProps } from "react-native";

export interface BoxProps extends ViewProps {

};

export const Box = (props: BoxProps) => {
	return (
		<View
			{...props}
		/>
	);
};
