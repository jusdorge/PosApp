package com.example.posapp.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import com.example.posapp.model.Customer;

public class LocationUtils {
    
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    
    /**
     * التحقق من وجود صلاحيات الموقع
     */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * طلب صلاحيات الموقع
     */
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    /**
     * الحصول على الموقع الحالي للمستخدم
     */
    public static Location getCurrentLocation(Context context) {
        if (!hasLocationPermission(context)) {
            return null;
        }
        
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }
        
        Location location = null;
        try {
            // محاولة الحصول على آخر موقع معروف من GPS
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            
            // إذا لم يتم العثور على موقع من GPS، جرب Network
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException e) {
            // يتم التعامل مع الاستثناء في حالة عدم وجود صلاحيات
            return null;
        }
        
        return location;
    }
    
    /**
     * فتح Google Maps مع المسار إلى العميل
     */
    public static void openNavigationToCustomer(Context context, Customer customer) {
        if (customer == null) {
            Toast.makeText(context, "بيانات العميل غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // التحقق من وجود موقع للعميل
        if (customer.getLatitude() == 0.0 && customer.getLongitude() == 0.0) {
            Toast.makeText(context, "موقع العميل غير محدد في النظام", Toast.LENGTH_LONG).show();
            return;
        }
        
        // جرب الدالة المحسنة أولاً
        try {
            openMapsWithMultipleFallbacks(context, customer);
        } catch (Exception e) {
            // إذا فشلت، جرب الطريقة المضمونة (Intent Chooser)
            openLocationWithChooser(context, customer.getLatitude(), customer.getLongitude(), customer.getName());
        }
    }
    
    /**
     * فتح Google Maps مع إحداثيات محددة
     */
    public static void openGoogleMaps(Context context, double latitude, double longitude, String locationName) {
        // إنشاء عميل مؤقت لاستخدام الدالة المحسنة
        Customer tempCustomer = new Customer();
        tempCustomer.setLatitude(latitude);
        tempCustomer.setLongitude(longitude);
        tempCustomer.setName(locationName != null ? locationName : "الموقع");
        
        openMapsWithMultipleFallbacks(context, tempCustomer);
    }
    
    /**
     * دالة محسنة لفتح Google Maps بطرق متعددة
     */
    public static void openMapsWithMultipleFallbacks(Context context, Customer customer) {
        if (customer == null) {
            Toast.makeText(context, "بيانات العميل غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double lat = customer.getLatitude();
        double lng = customer.getLongitude();
        String name = customer.getName();
        
        if (lat == 0.0 && lng == 0.0) {
            Toast.makeText(context, "موقع العميل غير محدد في النظام", Toast.LENGTH_LONG).show();
            return;
        }
        
        Toast.makeText(context, "جاري فتح الخرائط...", Toast.LENGTH_SHORT).show();
        
        // المحاولة الأولى: Google Maps Navigation
        if (tryGoogleMapsNavigation(context, lat, lng, name)) return;
        
        // المحاولة الثانية: Google Maps عادي
        if (tryGoogleMapsRegular(context, lat, lng, name)) return;
        
        // المحاولة الثالثة: أي تطبيق خرائط متوفر
        if (tryAnyMapsApp(context, lat, lng, name)) return;
        
        // المحاولة الأخيرة: المتصفح
        tryWebBrowser(context, lat, lng, name);
    }
    
    /**
     * التحقق من وجود Google Maps
     */
    private static boolean isGoogleMapsInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * دالة مبسطة لفتح الموقع (تحاول كل الطرق)
     */
    public static void openLocation(Context context, double latitude, double longitude, String locationName) {
        try {
            // أولاً: جرب Google Maps مباشرة
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", 
                latitude, longitude, latitude, longitude, locationName);
            intent.setData(Uri.parse(uri));
            
            context.startActivity(intent);
            Toast.makeText(context, "فتح " + locationName, Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            // إذا فشل، جرب المتصفح
            try {
                String webUrl = String.format(Locale.US, 
                    "https://www.google.com/maps/search/?api=1&query=%f,%f", latitude, longitude);
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                context.startActivity(webIntent);
                Toast.makeText(context, "فتح " + locationName + " في المتصفح", Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Toast.makeText(context, "لا يمكن فتح الخرائط", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private static boolean tryGoogleMapsNavigation(Context context, double lat, double lng, String name) {
        try {
            String uri = String.format(Locale.US, "google.navigation:q=%f,%f", lat, lng);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            
            context.startActivity(intent);
            Toast.makeText(context, "فتح المسار إلى " + name, Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            // فشل في فتح Google Maps Navigation
        }
        return false;
    }
    
    private static boolean tryGoogleMapsRegular(Context context, double lat, double lng, String name) {
        try {
            String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", 
                lat, lng, lat, lng, Uri.encode(name));
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            
            context.startActivity(intent);
            Toast.makeText(context, "فتح موقع " + name, Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            // فشل في فتح Google Maps العادي
        }
        return false;
    }
    
    private static boolean tryAnyMapsApp(Context context, double lat, double lng, String name) {
        try {
            String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", 
                lat, lng, lat, lng, Uri.encode(name));
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            
            context.startActivity(intent);
            Toast.makeText(context, "فتح موقع " + name + " في تطبيق الخرائط", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            // فشل في فتح أي تطبيق خرائط
        }
        return false;
    }
    
    private static void tryWebBrowser(Context context, double lat, double lng, String name) {
        // جرب عدة روابط مختلفة للمتصفح
        String[] browserUris = {
            String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng),
            String.format(Locale.US, "https://maps.google.com/maps?q=%f,%f", lat, lng),
            String.format(Locale.US, "https://www.google.com/maps?q=%f,%f", lat, lng)
        };
        
        for (String webUri : browserUris) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                // إضافة FLAG_ACTIVITY_NEW_TASK للتأكد من فتح المتصفح
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(webIntent);
                Toast.makeText(context, "فتح موقع " + name + " في المتصفح", Toast.LENGTH_SHORT).show();
                return; // نجح الفتح، اخرج من الدالة
                
            } catch (Exception e) {
                // جرب الرابط التالي
                continue;
            }
        }
        
        // إذا فشلت كل المحاولات
        Toast.makeText(context, "لم نتمكن من فتح الخرائط. تحقق من اتصالك بالإنترنت", Toast.LENGTH_LONG).show();
    }

    /**
     * حساب المسافة بين نقطتين بالكيلومتر
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // نصف قطر الأرض بالكيلومتر
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // المسافة بالكيلومتر
    }
    
    /**
     * تنسيق المسافة للعرض
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0f متر", distanceKm * 1000);
        } else {
            return String.format("%.1f كم", distanceKm);
        }
    }
    
    /**
     * دالة مضمونة لفتح الموقع (Intent Chooser)
     */
    public static void openLocationWithChooser(Context context, double latitude, double longitude, String locationName) {
        try {
            String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", 
                latitude, longitude, latitude, longitude, locationName);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            
            // إنشاء Intent Chooser ليظهر للمستخدم كل التطبيقات المتوفرة
            Intent chooser = Intent.createChooser(mapIntent, "اختر تطبيق الخرائط");
            
            if (chooser.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooser);
                Toast.makeText(context, "اختر التطبيق المناسب لفتح " + locationName, Toast.LENGTH_SHORT).show();
            } else {
                // إذا فشل حتى الـ Chooser، جرب المتصفح مباشرة
                openDirectInBrowser(context, latitude, longitude, locationName);
            }
        } catch (Exception e) {
            openDirectInBrowser(context, latitude, longitude, locationName);
        }
    }
    
    /**
     * فتح مباشر في المتصفح (مضمون)
     */
    private static void openDirectInBrowser(Context context, double latitude, double longitude, String locationName) {
        try {
            String url = String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f", latitude, longitude);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);  
            Toast.makeText(context, "فتح " + locationName + " في المتصفح", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * دالة لاختبار فتح الخرائط (للتطوير)
     */
    public static void testMapsOpening(Context context) {
        // إنشاء عميل تجريبي للاختبار
        Customer testCustomer = new Customer();
        testCustomer.setName("عميل تجريبي");
        testCustomer.setLatitude(36.7538); // الجزائر العاصمة
        testCustomer.setLongitude(3.0588);
        
        Toast.makeText(context, "اختبار فتح الخرائط...", Toast.LENGTH_SHORT).show();
        
        // جرب الطريقة الأكثر ضماناً
        openLocationWithChooser(context, testCustomer.getLatitude(), testCustomer.getLongitude(), testCustomer.getName());
    }
}