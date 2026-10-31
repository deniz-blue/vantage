package blue.deniz.vantage.app

import blue.deniz.vantage.core.Language
import blue.deniz.vantage.core.Languages

fun contentLanguage(code: String): Language = Languages.of(code, languageAutonym(code))
