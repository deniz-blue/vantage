import { Pressable, StyleSheet, type ViewStyle } from "react-native";
import { IconPlus, IconX } from "@tabler/icons-react-native";
import { ActionIcon, type ActionIconProps } from "./ActionIcon";
import { resolveColor } from "../../theme/colors";
import { Radius } from "../../theme/sizing";
import { ReactNode, useState } from "react";
import { Box, BoxProps } from "./Box";
import { Button, ButtonProps } from "./Button";
import { Text } from "./Text";

export interface FabAction extends Omit<ButtonProps, "children"> {
	label?: ReactNode;
};

export interface FabProps extends Omit<ActionIconProps, "children"> {
	wrapperProps?: BoxProps;
	icon?: React.ReactNode;
	color?: string;
	actions?: FabAction[];
}

const shadow: ViewStyle = {
	elevation: 6,
	shadowColor: "#000",
	shadowOffset: { width: 0, height: 3 },
	shadowOpacity: 0.3,
	shadowRadius: 4,
};

const FAB_SIZE = 56;

export const Fab = ({
	icon,
	color = "Primary",
	style,
	actions = [],
	wrapperProps: { style: wrapperStyle, ...wrapperProps } = {},
	...rest
}: FabProps) => {
	const [open, setOpen] = useState(false);

	return (
		<>
			{open && (
				<Box
					component={Pressable}
					pos="absolute"
					top={0}
					left={0}
					right={0}
					bottom={0}
					style={{ zIndex: 99 }}
					bg="Black"
					op={0.2}
					onPress={() => setOpen(false)}
				/>
			)}

			<Box
				pos="absolute"
				right={16}
				bottom={16}
				align="flex-end"
				gap={8}
				style={[{ zIndex: 100 }, wrapperStyle]}
				{...wrapperProps}
			>
				{open && actions.map(({ label, ...action }, index) => (
					<Button
						key={index}
						style={[shadow, style]}
						variant="primary"
						size="lg"
						radius={Radius.xl}
						children={(
							<Text c="White">
								{label}
							</Text>
						)}
						{...action}
					/>
				))}

				<ActionIcon
					w={FAB_SIZE}
					h={FAB_SIZE}
					radius={open ? "50%" : Radius.xl}
					op={open ? 0.7 : 1}
					bg={resolveColor(color)}
					style={[shadow, style]}
					onPress={() => {
						setOpen((prev) => !prev);
						rest.onPress?.();
					}}
					{...(rest as any)}
				>
					{open ? <IconX /> : icon}
				</ActionIcon>
			</Box>
		</>
	);
};
