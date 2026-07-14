import { MaybeTranslations } from "@evnt/translations";
import { Text, TextProps } from "../base/Text";
import { ReactNode, useMemo } from "react";
import { useLocaleStore } from "../../stores/useLocaleStore";

export interface TransTextProps extends Omit<TextProps, "children"> {
	value: MaybeTranslations;
	fallback?: ReactNode;
}

export const TransText = ({ value, fallback, ...rest }: TransTextProps) => {
	const userLanguage = useLocaleStore((s) => s.language);

	const { lang, text } = useMemo(() => {
		if (!value) return { lang: "", text: "" };
		if (value[userLanguage]) return { lang: userLanguage, text: value[userLanguage] };
		const firstLang = Object.keys(value)[0];
		if (value[firstLang]) return { lang: firstLang, text: value[firstLang] };
		return { lang: "en", text: fallback ?? "" };
	}, [userLanguage, value]);

	if (!text) return null;

	return <Text accessibilityLanguage={lang} {...rest} children={text} />;
};
