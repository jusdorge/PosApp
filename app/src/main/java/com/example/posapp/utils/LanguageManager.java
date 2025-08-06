package com.example.posapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * مدير اللغات - للتحكم في لغة التطبيق ودعم عدة لغات
 */
public class LanguageManager {
    
    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";
    
    // اللغات المدعومة
    public static final String LANGUAGE_ARABIC = "ar";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_AUTO = "auto"; // لغة النظام
    
    private static LanguageManager instance;
    private SharedPreferences prefs;
    
    private LanguageManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * حفظ اللغة المختارة
     */
    public void setLanguage(String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        android.util.Log.d("LanguageManager", "تم حفظ اللغة: " + languageCode);
    }
    
    /**
     * الحصول على اللغة المحفوظة
     */
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_AUTO);
    }
    
    /**
     * تطبيق اللغة على السياق
     */
    public Context applyLanguage(Context context) {
        String languageCode = getLanguage();
        
        if (LANGUAGE_AUTO.equals(languageCode)) {
            // استخدام لغة النظام
            return context;
        }
        
        return updateContext(context, languageCode);
    }
    
    /**
     * تطبيق اللغة على النشاط
     */
    public void applyLanguage(Activity activity) {
        String languageCode = getLanguage();
        
        if (LANGUAGE_AUTO.equals(languageCode)) {
            return; // استخدام لغة النظام
        }
        
        updateActivityContext(activity, languageCode);
    }
    
    /**
     * تحديث السياق بلغة محددة
     */
    private Context updateContext(Context context, String languageCode) {
        Locale locale = Locale.forLanguageTag(languageCode);
        if (locale.getLanguage().isEmpty()) {
            // fallback for older language codes
            locale = new Locale(languageCode);
        }
        Locale.setDefault(locale);
        
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);
        } else {
            configuration.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(locale);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration);
        } else {
            context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
            return context;
        }
    }
    
    /**
     * تحديث النشاط بلغة محددة
     */
    private void updateActivityContext(Activity activity, String languageCode) {
        Locale locale = Locale.forLanguageTag(languageCode);
        if (locale.getLanguage().isEmpty()) {
            // fallback for older language codes
            locale = new Locale(languageCode);
        }
        Locale.setDefault(locale);
        
        Resources resources = activity.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);
        } else {
            configuration.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(locale);
            }
        }
        
        resources.updateConfiguration(configuration, displayMetrics);
        
        android.util.Log.d("LanguageManager", "تم تطبيق اللغة على النشاط: " + languageCode);
    }
    
    /**
     * التحقق من كون اللغة الحالية RTL
     */
    public boolean isRTL() {
        String currentLanguage = getLanguage();
        if (LANGUAGE_AUTO.equals(currentLanguage)) {
            // فحص لغة النظام
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return Resources.getSystem().getConfiguration().getLayoutDirection() == 
                       android.view.View.LAYOUT_DIRECTION_RTL;
            } else {
                return false;
            }
        }
        
        return LANGUAGE_ARABIC.equals(currentLanguage);
    }
    
    /**
     * الحصول على اسم اللغة للعرض
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case LANGUAGE_ARABIC:
                return "العربية";
            case LANGUAGE_ENGLISH:
                return "English";
            case LANGUAGE_AUTO:
                return "تلقائي / Auto";
            default:
                return languageCode;
        }
    }
    
    /**
     * الحصول على قائمة اللغات المدعومة
     */
    public String[] getSupportedLanguages() {
        return new String[]{LANGUAGE_AUTO, LANGUAGE_ARABIC, LANGUAGE_ENGLISH};
    }
    
    /**
     * الحصول على أسماء اللغات للعرض
     */
    public String[] getSupportedLanguageNames() {
        String[] codes = getSupportedLanguages();
        String[] names = new String[codes.length];
        for (int i = 0; i < codes.length; i++) {
            names[i] = getLanguageDisplayName(codes[i]);
        }
        return names;
    }
    
    /**
     * إعادة تشغيل النشاط لتطبيق اللغة الجديدة
     */
    public void restartActivity(Activity activity) {
        android.util.Log.d("LanguageManager", "إعادة تشغيل النشاط لتطبيق اللغة الجديدة");
        activity.recreate();
    }
    
    /**
     * تطبيق RTL على View
     */
    public void applyRTL(android.view.View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (isRTL()) {
                view.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
                view.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);
            } else {
                view.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
                view.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);
            }
        }
    }
}