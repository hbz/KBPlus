package de.laser.base

import de.laser.I10nTranslation
import org.springframework.context.i18n.LocaleContextHolder

abstract class AbstractI10nTranslatable {
    public static final LOCALE_DE = Locale.GERMAN.toString()
    public static final LOCALE_EN = Locale.ENGLISH.toString()

    protected i10nStorage = [:]

    // get translation; current locale
    def getI10n(String property) {
        getI10n(property, LocaleContextHolder.getLocale().toString())
    }

    // get translation
    def getI10n(String property, Locale locale) {
        getI10n(property, locale.toString())
    }
    // get translation
    def getI10n(String property, String locale) {
        def result
        locale = I10nTranslation.decodeLocale(locale)

        if (I10nTranslation.supportedLocales.contains(locale)) {
            result = this."${property}_${locale}"
        }
        else {
            result = "- requested locale ${locale} not supported -"
        }
        result = (result != 'null') ? result : ''
    }

    // returning virtual property for template tags
    def propertyMissing(String name) {
        if (! i10nStorage.containsKey(name)) {

            def parts = name.split("_")
            if (parts.size() == 2) {
                def fallback = this."${parts[0]}"
                def i10n = I10nTranslation.get(this, parts[0], parts[1])
                this."${name}" = (i10n ? i10n : "${fallback}")
            }
        }

        i10nStorage["${name}"]
    }

    // setting virtual property
    def propertyMissing(String name, def value) {
        i10nStorage["${name}"] = value
    }

    /** Suffix for DB Tables. Right now, there are only German and English Columns */
    static String getLanguageSuffix(){
        String languageSuffix = 'en'
        if (LocaleContextHolder.getLocale().getLanguage() == Locale.GERMAN.getLanguage()){
            languageSuffix = 'de'
        }
        languageSuffix
    }
}
