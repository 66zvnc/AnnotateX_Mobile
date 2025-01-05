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
            case "English":
                return new Locale("en");
            case "Español":
                return new Locale("es");
            case "Français":
                return new Locale("fr");
            case "Deutsch":
                return new Locale("de");
            case "中文":
                return Locale.SIMPLIFIED_CHINESE;
            case "日本語":
                return Locale.JAPANESE;
            case "Български":
                return new Locale("bg");
            default:
                return new Locale("en");
        }
    }

    public static String getCurrentLanguageName(Context context) {
        Locale currentLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentLocale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = context.getResources().getConfiguration().locale;
        }

        // Map locale back to display name
        if (currentLocale.getLanguage().equals("en")) return "English";
        if (currentLocale.getLanguage().equals("es")) return "Español";
        if (currentLocale.getLanguage().equals("fr")) return "Français";
        if (currentLocale.getLanguage().equals("de")) return "Deutsch";
        if (currentLocale.getLanguage().equals("zh")) return "中文";
        if (currentLocale.getLanguage().equals("ja")) return "日本語";
        if (currentLocale.getLanguage().equals("bg")) return "Български";
        
        return "English"; //  fallback
    }
} 