import { Fragment, useState } from "react";
import { Colors } from "../../../theme/colors";
import { FontSize, Radius } from "../../../theme/sizing";
import { Box } from "../Box";
import { InputWrapper, InputWrapperProps } from "./InputWrapper";
import {
	TextInput as RNTextInput,
	type TextInputProps as RNTextInputProps,
} from "react-native";
import { Spacing } from "../../../theme/spacing";

export interface TextInputSegment extends Omit<RNTextInputProps, "placeholderTextColor"> {

};

export interface SegmentedTextInputProps extends Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
	segments: TextInputSegment[];
	separator?: React.ReactNode;
	common?: Omit<RNTextInputProps, "placeholderTextColor">;
};

export const SegmentedTextInput = ({
	segments,
	separator,
	common: { style: commonStyle, ...commonProps } = {},
	description,
	error,
	label,
	required,
}: SegmentedTextInputProps) => {
	const [focused, setFocused] = useState<number[]>([]);

	return (
		<InputWrapper
			label={label}
			description={description}
			error={error}
			required={required}
		>
			<Box
				direction="row"
				align="center"
				gap="sm"
				px="sm"
				style={[
					{
						backgroundColor: Colors.BackgroundInput,
						borderRadius: Radius.Default,
						borderWidth: 1,
						borderColor: error ? "#f44336" : "transparent",
						outlineWidth: focused.length ? 2 : 0,
						outlineStyle: "solid",
						outlineColor: Colors.Primary,
					},
				]}
			>
				{segments.map(({ style, ...segment }, index) => (
					<Fragment key={index}>
						<RNTextInput
							placeholderTextColor={Colors.TextDimmed}
							style={[
								{
									color: Colors.Text,
									backgroundColor: "transparent",
									paddingVertical: Spacing.xs,
									fontSize: FontSize.lg,
									fontFamily: "Lexend",
									outlineWidth: 0,
								},
								commonStyle,
								style,
							]}
							onFocus={() => setFocused(prev => [...prev, index])}
							onBlur={() => setFocused(prev => prev.filter(f => f !== index))}
							{...commonProps}
							{...segment}
						/>
						{index < segments.length - 1 && (
							separator
						)}
					</Fragment>
				))}
			</Box>
		</InputWrapper>
	);
};
