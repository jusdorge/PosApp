# إعداد دعم عدة لغات - POS App

## 🌐 اللغات المدعومة

تم إعداد التطبيق ليدعم اللغات التالية:
- **العربية** (ar) - مع دعم RTL
- **الإنجليزية** (en) - LTR
- **تلقائي** (auto) - يتبع لغة النظام

## 📁 هيكل الملفات

### مجلدات الموارد
```
app/src/main/res/
├── values/                 # الافتراضي (عربي)
│   ├── strings.xml
│   └── config.xml
├── values-ar/             # العربية مع RTL
│   ├── strings.xml
│   └── config.xml
└── values-en/             # الإنجليزية
    └── strings.xml
```

### الكلاسات المساعدة
- `LanguageManager.java` - إدارة اللغة وتطبيقها
- `LanguageSelectionDialog.java` - حوار اختيار اللغة

## 🛠️ المكونات المطبقة

### 1. LanguageManager
```java
// الحصول على مدير اللغة
LanguageManager languageManager = LanguageManager.getInstance(this);

// تطبيق اللغة المختارة
languageManager.applyLanguage(this);

// تغيير اللغة
languageManager.setLanguage(LanguageManager.LANGUAGE_ARABIC);

// فحص RTL
boolean isRTL = languageManager.isRTL();
```

### 2. دعم RTL
- تم إضافة `android:supportsRtl="true"` في AndroidManifest.xml
- إعداد `config.xml` في مجلد values-ar للـ RTL
- تطبيق RTL تلقائياً عند اختيار العربية

### 3. حوار اختيار اللغة
```java
// عرض حوار اختيار اللغة
LanguageSelectionDialog dialog = LanguageSelectionDialog.newInstance();
dialog.setOnLanguageSelectedListener(languageCode -> {
    // تطبيق اللغة الجديدة
    getActivity().recreate();
});
dialog.show(getFragmentManager(), "LanguageDialog");
```

## 🎨 واجهة المستخدم

### زر تغيير اللغة
تم إضافة كارت "🌐 تغيير اللغة" في MoreFragment:
- متاح لجميع المستخدمين
- يفتح حوار اختيار اللغة
- يطبق اللغة الجديدة فوراً

### تطبيق اللغة في MainActivity
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // تطبيق اللغة قبل إعداد UI
    LanguageManager languageManager = LanguageManager.getInstance(this);
    languageManager.applyLanguage(this);
    
    // باقي الكود...
}
```

## 📋 النصوص المتوفرة

### النصوص الأساسية
- أسماء التطبيق والقوائم
- أزرار التنقل السفلي
- عناوين الأنشطة
- رسائل الحالة

### النصوص المتخصصة
- خدمات الموقع
- تعديل الفواتير
- إدارة المستخدمين
- تصدير/استيراد البيانات
- الطباعة
- التقارير المالية
- الشبكة والعمل بدون اتصال

### رسائل اختيار اللغة
- عناوين الحوارات
- أسماء اللغات
- رسائل التأكيد
- تعليمات إعادة التشغيل

## 🔄 كيفية إضافة لغة جديدة

### 1. إنشاء مجلد الموارد
```bash
mkdir app/src/main/res/values-[language_code]
```

### 2. إنشاء strings.xml
```xml
<resources>
    <string name="app_name">App Name in New Language</string>
    <!-- باقي النصوص... -->
</resources>
```

### 3. إضافة اللغة للـ LanguageManager
```java
public static final String LANGUAGE_NEW = "new";

public String getLanguageDisplayName(String languageCode) {
    switch (languageCode) {
        case LANGUAGE_NEW:
            return "New Language";
        // باقي اللغات...
    }
}
```

### 4. تحديث قائمة اللغات المدعومة
```java
public String[] getSupportedLanguages() {
    return new String[]{
        LANGUAGE_AUTO, 
        LANGUAGE_ARABIC, 
        LANGUAGE_ENGLISH, 
        LANGUAGE_NEW  // اللغة الجديدة
    };
}
```

## 🧪 اختبار الدعم متعدد اللغات

### 1. اختبار التبديل بين اللغات
1. افتح التطبيق
2. اذهب إلى "المزيد"
3. اضغط على "🌐 تغيير اللغة"
4. اختر لغة مختلفة
5. اضغط "إعادة تشغيل التطبيق"
6. تأكد من تطبيق اللغة الجديدة

### 2. اختبار RTL
1. اختر اللغة العربية
2. تأكد من:
   - محاذاة النصوص من اليمين
   - ترتيب العناصر من اليمين لليسار
   - اتجاه الأيقونات الصحيح

### 3. اختبار لغة النظام
1. اختر "تلقائي"
2. غير لغة النظام في الإعدادات
3. أعد تشغيل التطبيق
4. تأكد من متابعة لغة النظام

## 🔧 النصائح والإرشادات

### لضمان ترجمة صحيحة:
1. **استخدم getString()** بدلاً من النصوص المباشرة
2. **نظم strings.xml** بتصنيفات واضحة
3. **اختبر جميع الشاشات** بعد إضافة لغة جديدة
4. **تأكد من RTL** للغات التي تكتب من اليمين لليسار

### للنصوص المتحركة:
```java
// استخدم placeholders للنصوص المتغيرة
String message = getString(R.string.welcome_user, userName);

// في strings.xml:
<string name="welcome_user">مرحباً %1$s</string>
```

### للتواريخ والأرقام:
```java
// استخدم Locale المناسب
Locale currentLocale = getResources().getConfiguration().locale;
DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale);
```

## 🎯 الحالة الحالية

### ✅ مُطبق ومكتمل:
- [x] إعداد ملفات strings.xml للعربية والإنجليزية
- [x] إنشاء LanguageManager للتحكم في اللغة
- [x] إضافة LanguageSelectionDialog
- [x] دمج زر تغيير اللغة في MoreFragment
- [x] دعم RTL للعربية
- [x] تطبيق اللغة في MainActivity
- [x] تنظيم النصوص بتصنيفات واضحة

### 🔄 للتحسين المستقبلي:
- [ ] استخراج النصوص المتبقية من ملفات Java
- [ ] إضافة ترجمة لرسائل Toast
- [ ] دعم المزيد من اللغات (فرنسية، إسبانية...)
- [ ] تطبيق Locale على التواريخ والأرقام
- [ ] إضافة اختبارات تلقائية للترجمة

## 🚀 بدء الاستخدام

1. **تشغيل التطبيق**
2. **الذهاب إلى "المزيد"**
3. **اختيار "🌐 تغيير اللغة"**
4. **اختيار اللغة المطلوبة**
5. **إعادة تشغيل التطبيق**

النظام جاهز للاستخدام مع دعم كامل للعربية والإنجليزية! 🎉

## 📞 الدعم

إذا واجهت مشاكل في:
- عدم تطبيق اللغة: تأكد من إعادة تشغيل التطبيق
- مشاكل RTL: تحقق من إعدادات `config.xml`
- نصوص مفقودة: تأكد من وجودها في جميع ملفات `strings.xml`