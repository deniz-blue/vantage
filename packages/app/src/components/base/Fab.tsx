import { Pressable, type ViewStyle } from "react-native";
import Animated, { FadeIn } from "react-native-reanimated";
import { IconX } from "@tabler/icons-react-native";
import { ActionIcon, type ActionIconProps } from "./button/ActionIcon";
import { resolveColor } from "../../theme/colors";
import { Radius } from "../../theme/sizing";
import { type ReactNode, useState } from "react";
import { Box, type BoxProps } from "./Box";
import { Button, type ButtonProps } from "./button/Button";
import { Text } from "./Text";

export interface FabAction extends Omit<ButtonProps, "children"> {
	label?: ReactNode;
}

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
				style={[{ zIndex: 100 }, wrapperStyle]}
				{...wrapperProps}
			>
				{open && (
					<Box direction="column">
						{actions.map(({ label, onPress, ...action }, index) => (
							<Animated.View key={index} entering={FadeIn}>
								<Button
									style={[shadow, style]}
									onPress={(e) => {
										onPress?.(e);
										setOpen(false);
									}}
									variant="primary"
									size="lg"
									radius={Radius.xl}
									mb="sm"
									children={<Text c="White">{label}</Text>}
									{...action}
								/>
							</Animated.View>
						))}
					</Box>
				)}

				<ActionIcon
					w={FAB_SIZE}
					h={FAB_SIZE}
					radius={open ? FAB_SIZE / 2 : Radius.xl}
					op={open ? 0.7 : 1}
					bg={resolveColor(color)}
					style={[shadow, style]}
					onPress={() => {
						setOpen((prev) => !prev);
						rest.onPress?.();
					}}
					{...(rest as any)}
				>
					{open ? <IconX width={28} height={28} color="#fff" /> : icon}
				</ActionIcon>
			</Box>
		</>
	);
};
