import { useMemo } from "react";
import { TranslationsUtil } from "@evnt/translations";
import { useLocaleStore } from "../stores/useLocaleStore";

export function useTranslator() {
	const language = useLocaleStore((s) => s.language);

	const translator = useMemo(
		() => TranslationsUtil.createTranslator([language]),
		[language],
	);

	return translator;
}
