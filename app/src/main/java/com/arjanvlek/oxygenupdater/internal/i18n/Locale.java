package com.arjanvlek.oxygenupdater.internal.i18n;

import static com.arjanvlek.oxygenupdater.ApplicationData.LOCALE_DUTCH;

public enum Locale {

    NL, EN;

    public static Locale getLocale() {
        String appLocale = java.util.Locale.getDefault().getDisplayLanguage();

        if(appLocale.equals(LOCALE_DUTCH)) {
            return NL;
        }

        return EN;
    }
}
