package com.example.islamicquiz.model;

public enum PaymentMethod {
    CASH("نقداً", "💵"),
    CREDIT("ديناً", "📝"),
    CARD("بطاقة ائتمان", "💳"),
    BANK_TRANSFER("تحويل بنكي", "🏦"),
    DIGITAL_WALLET("محفظة رقمية", "📱"),
    CHECK("شيك", "📄");

    private final String displayName;
    private final String icon;

    PaymentMethod(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayWithIcon() {
        return icon + " " + displayName;
    }

    // تحويل من النظام القديم (isPaid) إلى النظام الجديد
    public static PaymentMethod fromLegacyPaid(boolean isPaid) {
        return isPaid ? CASH : CREDIT;
    }

    // للتوافق مع النظام القديم
    public boolean isLegacyPaid() {
        return this == CASH || this == CARD || this == BANK_TRANSFER || 
               this == DIGITAL_WALLET || this == CHECK;
    }
    
    // هل هذه الطريقة دين
    public boolean isDebt() {
        return this == CREDIT;
    }
    
    // هل هذه الطريقة نقدية (فورية)
    public boolean isImmediate() {
        return !isDebt();
    }
} 