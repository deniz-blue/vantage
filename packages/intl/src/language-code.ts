export class UtilLanguageCode {
	static isValidLanguageCode(code: string) {
		try {
			new Intl.Locale(code);
			return true;
		} catch {
			return false;
		}
	}

	static getNameIn(lang: string, inLang: string): string | null {
		try {
			return new Intl.DisplayNames(inLang, { type: "language" }).of(lang) ?? null;
		} catch {
			return null;
		}
	}

	static getAutonym(lang: string): string | null {
		return this.getNameIn(lang, lang);
	}

	static getEnglishName(lang: string): string | null {
		return this.getNameIn(lang, "en");
	}

	static getLabel(lang: string): string {
		const autonym = this.getAutonym(lang);
		const englishName = this.getEnglishName(lang);
		return [...new Set([autonym, englishName])].filter(Boolean).join(" / ") || lang;
	}
}
