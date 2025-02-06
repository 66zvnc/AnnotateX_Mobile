package com.example.annotatex_mobile;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.annotatex_mobile.utils.LanguageUtils;

public class AnnotateXApplication extends Application {
    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_DARK_MODE = "darkMode";

    @Override
    public void onCreate() {
        super.onCreate();
        initializeTheme();
    }

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String language = prefs.getString("selected_language", "English (US)");
        super.attachBaseContext(LanguageUtils.updateLocale(base, language));
    }

    private void initializeTheme() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
} 