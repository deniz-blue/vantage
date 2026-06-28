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
import { InputBase } from "./InputBase";

export interface TextInputSegment extends Omit<RNTextInputProps, "placeholderTextColor"> {
	renderAfter?: React.ReactNode;
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
			<InputBase
				gap="xs"
				px="sm"
				focused={focused.length > 0}
				style={{
					borderWidth: 1,
					borderColor: error ? "#f44336" : "transparent",
				}}
			>
				{segments.map(({ renderAfter, style, ...segment }, index) => (
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
						{renderAfter}
					</Fragment>
				))}
			</InputBase>
		</InputWrapper>
	);
};
