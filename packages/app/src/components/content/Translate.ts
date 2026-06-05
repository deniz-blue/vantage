import { Translations, TranslationsUtil } from "@evnt/translations";
import { useUserLanguage } from "../../stores/locale";
import { useMemo } from "react";

export const Translate = ({ value }: { value?: Translations | null }) => {
	const userLanguage = useUserLanguage();
	const text = useMemo(() => TranslationsUtil.translate(value, [userLanguage]), [value, userLanguage]);
	return text;
};
