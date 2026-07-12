import { MaybeTranslations } from "@evnt/translations";
import { Text, TextProps } from "../base/Text";
import { useTranslator } from "../../hooks/useTranslator";
import { ReactNode } from "react";

export interface TransTextProps extends Omit<TextProps, "children"> {
	value: MaybeTranslations;
	fallback?: ReactNode;
}

export const TransText = ({ value, fallback, ...rest }: TransTextProps) => {
	const translate = useTranslator();

	return <Text {...rest} children={translate(value) || fallback} />;
};
