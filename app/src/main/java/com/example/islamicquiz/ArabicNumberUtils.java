package com.example.islamicquiz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * فئة مساعدة لتحويل الأرقام من الهندية إلى العربية في النصوص
 */
public class ArabicNumberUtils {
    
    // مصفوفة الأرقام الهندية-العربية (المستخدمة في Locale("ar"))
    private static final char[] HINDI_DIGITS = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
    
    // مصفوفة الأرقام العربية (المطلوبة)
    private static final char[] ARABIC_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    
    /**
     * تحويل النص من الأرقام الهندية إلى الأرقام العربية
     */
    public static String convertToArabicNumbers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            boolean found = false;
            // البحث عن الرقم الهندي وتحويله للعربي
            for (int i = 0; i < HINDI_DIGITS.length; i++) {
                if (c == HINDI_DIGITS[i]) {
                    result.append(ARABIC_DIGITS[i]);
                    found = true;
                    break;
                }
            }
            // إذا لم يكن رقماً هندياً، احتفظ بالحرف كما هو
            if (!found) {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * تنسيق التاريخ بالعربية مع أرقام عربية
     */
    public static String formatDateWithArabicNumbers(Date date, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, new Locale("ar", "SA"));
        String formattedDate = dateFormat.format(date);
        return convertToArabicNumbers(formattedDate);
    }
    
    /**
     * تنسيق التاريخ الطويل (اسم اليوم + التاريخ) بأرقام عربية
     */
    public static String formatLongDateWithArabicNumbers(Date date) {
        return formatDateWithArabicNumbers(date, "EEEE, dd MMMM yyyy");
    }
    
    /**
     * تنسيق التاريخ القصير بأرقام عربية
     */
    public static String formatShortDateWithArabicNumbers(Date date) {
        return formatDateWithArabicNumbers(date, "yyyy/MM/dd");
    }
    
    /**
     * تنسيق التاريخ والوقت بأرقام عربية
     */
    public static String formatDateTimeWithArabicNumbers(Date date) {
        return formatDateWithArabicNumbers(date, "yyyy/MM/dd HH:mm");
    }
    
    /**
     * تنسيق التاريخ للطباعة بأرقام عربية
     */
    public static String formatPrintDateWithArabicNumbers(Date date) {
        return formatDateWithArabicNumbers(date, "dd/MM/yyyy HH:mm");
    }
}