package com.example.posapp;

/**
 * فئة مساعدة لتنسيق العملة الجزائرية (الدينار الجزائري)
 */
public class CurrencyUtils {
    
    // رمز العملة الجزائرية
    public static final String CURRENCY_SYMBOL = "دج";
    public static final String CURRENCY_CODE = "DZD";
    public static final String CURRENCY_NAME = "دينار جزائري";
    
    /**
     * تنسيق المبلغ بالدينار الجزائري
     * @param amount المبلغ
     * @return النص المُنسق مثل "150.50 دج"
     */
    public static String formatCurrency(double amount) {
        return String.format("%.2f %s", amount, CURRENCY_SYMBOL);
    }
    
    /**
     * تنسيق المبلغ بالدينار الجزائري مع فاصلة للآلاف
     * @param amount المبلغ
     * @return النص المُنسق مثل "1,250.50 دج"
     */
    public static String formatCurrencyWithCommas(double amount) {
        return String.format("%,.2f %s", amount, CURRENCY_SYMBOL);
    }
    
    /**
     * تنسيق المبلغ للتقارير (بدون مسافات للتوافق مع الكود الحالي)
     * @param amount المبلغ
     * @return النص المُنسق مثل "150.50دج"
     */
    public static String formatCurrencyForReports(double amount) {
        return String.format("%.2f%s", amount, CURRENCY_SYMBOL);
    }
    
    /**
     * تنسيق المبلغ مع الرمز الدولي DZ
     * @param amount المبلغ
     * @return النص المُنسق مثل "DZ150.50"
     */
    public static String formatWithInternationalCode(double amount) {
        return String.format("DZ%.2f", amount);
    }
    
    /**
     * تنسيق المبلغ بإضافة إشارة + أو - للدفعات والديون
     * @param amount المبلغ
     * @param isPayment هل هو دفعة (true) أم دين (false)
     * @return النص المُنسق مثل "+ 150.50 دج" أو "- 150.50 دج"
     */
    public static String formatWithSign(double amount, boolean isPayment) {
        String sign = isPayment ? "+ " : "- ";
        return sign + formatCurrency(Math.abs(amount));
    }
    
    /**
     * تنسيق للعرض في الواجهات (مع مسافة واضحة)
     * @param amount المبلغ
     * @return النص المُنسق مثل "150.50 دج"
     */
    public static String formatForDisplay(double amount) {
        return formatCurrency(amount);
    }
    
    /**
     * تنسيق مختصر للمبالغ الكبيرة
     * @param amount المبلغ
     * @return النص المُنسق مثل "1.5ك دج" للآلاف، "1.2م دج" للملايين
     */
    public static String formatShortCurrency(double amount) {
        if (amount >= 1_000_000) {
            return String.format("%.1fم %s", amount / 1_000_000, CURRENCY_SYMBOL);
        } else if (amount >= 1_000) {
            return String.format("%.1fك %s", amount / 1_000, CURRENCY_SYMBOL);
        } else {
            return formatCurrency(amount);
        }
    }
}