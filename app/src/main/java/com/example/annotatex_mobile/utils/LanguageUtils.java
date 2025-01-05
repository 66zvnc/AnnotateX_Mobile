package com.example.annotatex_mobile.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public class LanguageUtils {
    
    public static Context updateLocale(Context context, String languageCode) {
        Locale locale = mapLanguageCodeToLocale(languageCode);
        Locale.setDefault(locale);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use the new Android 13+ APIs for better locale handling
            LocaleListCompat appLocale = LocaleListCompat.create(locale);
            AppCompatDelegate.setApplicationLocales(appLocale);
            return context;
        } else {
            Resources resources = context.getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            configuration.setLocale(locale);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList localeList = new LocaleList(locale);
                LocaleList.setDefault(localeList);
                configuration.setLocales(localeList);
            }
            
            return context.createConfigurationContext(configuration);
        }
    }

    private static Locale mapLanguageCodeToLocale(String languageCode) {
        switch (languageCode) {
            case "English (US)":
                return new Locale("en", "US");
            case "English (UK)":
                return new Locale("en", "GB");
            case "Bulgarian":
                return new Locale("bg");
            case "German":
                return new Locale("de");
            case "Russian":
                return new Locale("ru");
            case "French":
                return new Locale("fr");
            case "Spanish":
                return new Locale("es");
            // Add other languages as needed
            default:
                return new Locale("en", "US"); // Default to US English
        }
    }

    public static String getCurrentLanguageName(Context context) {
        Locale currentLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentLocale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = context.getResources().getConfiguration().locale;
        }

        String language = currentLocale.getLanguage();
        String country = currentLocale.getCountry();

        // Map locale back to display name
        if (language.equals("en")) {
            if (country.equals("US")) return "English (US)";
            if (country.equals("GB")) return "English (UK)";
            return "English (US)"; // Default if no country specified
        }
        if (language.equals("bg")) return "Bulgarian";
        if (language.equals("de")) return "German";
        if (language.equals("ru")) return "Russian";
        if (language.equals("fr")) return "French";
        if (language.equals("es")) return "Spanish";
        
        return "English (US)"; // Default fallback
    }
} 