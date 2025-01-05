package com.example.annotatex_mobile;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.annotatex_mobile.utils.LanguageUtils;

public class AnnotateXApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String language = prefs.getString("selected_language", "English (US)");
        super.attachBaseContext(LanguageUtils.updateLocale(base, language));
    }
} 