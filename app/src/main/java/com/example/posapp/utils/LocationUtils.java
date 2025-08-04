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
        
        try {
            // الحصول على الموقع الحالي
            Location currentLocation = getCurrentLocation(context);
            
            String navigationUri;
            if (currentLocation != null) {
                // إنشاء رابط المسار من الموقع الحالي إلى العميل
                navigationUri = String.format(Locale.US,
                    "google.navigation:q=%f,%f&mode=d",
                    customer.getLatitude(),
                    customer.getLongitude()
                );
            } else {
                // إذا لم نتمكن من الحصول على الموقع الحالي، افتح موقع العميل فقط
                navigationUri = String.format(Locale.US,
                    "geo:%f,%f?q=%f,%f(%s)",
                    customer.getLatitude(),
                    customer.getLongitude(),
                    customer.getLatitude(),
                    customer.getLongitude(),
                    Uri.encode(customer.getName())
                );
                Toast.makeText(context, "لم يتم العثور على موقعك الحالي، سيتم عرض موقع العميل فقط", Toast.LENGTH_LONG).show();
            }
            
            // فتح Google Maps
            openGoogleMapsWithUri(context, navigationUri, customer.getName());
            
        } catch (Exception e) {
            Toast.makeText(context, "خطأ في فتح الخريطة: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // محاولة بديلة باستخدام المتصفح
            openGoogleMapsInBrowser(context, customer);
        }
    }
    
    /**
     * فتح Google Maps مع إحداثيات محددة
     */
    public static void openGoogleMaps(Context context, double latitude, double longitude, String locationName) {
        try {
            String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", 
                    latitude, longitude, latitude, longitude, 
                    locationName != null ? Uri.encode(locationName) : Uri.encode("الموقع"));
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            
            // محاولة فتح Google Maps أولاً
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return;
            }
            
            // إذا لم تكن Google Maps مثبتة، جرب بدون تحديد package
            intent.setPackage(null);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return;
            }
            
            // كحل أخير، استخدم المتصفح
            openGoogleMapsInBrowser(context, latitude, longitude, locationName);
            
        } catch (Exception e) {
            Toast.makeText(context, "خطأ في فتح الخريطة: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * فتح Google Maps مع URI مخصص
     */
    private static void openGoogleMapsWithUri(Context context, String uri, String customerName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            
            // محاولة فتح Google Maps أولاً
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Toast.makeText(context, "فتح المسار إلى " + customerName, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // إذا لم تكن Google Maps مثبتة، جرب بدون تحديد package
            intent.setPackage(null);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Toast.makeText(context, "فتح المسار إلى " + customerName, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // كحل أخير، استخدم المتصفح
            openGoogleMapsInBrowser(context, customerName);
            
        } catch (Exception e) {
            Toast.makeText(context, "خطأ في فتح الخريطة: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * فتح Google Maps في المتصفح كحل بديل
     */
    private static void openGoogleMapsInBrowser(Context context, Customer customer) {
        try {
            String webUri = String.format(Locale.US, 
                "https://www.google.com/maps/search/?api=1&query=%f,%f",
                customer.getLatitude(), customer.getLongitude());
            
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            if (webIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(webIntent);
                Toast.makeText(context, "فتح المسار في المتصفح إلى " + customer.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "لا يمكن فتح الخرائط. تأكد من تثبيت Google Maps أو متصفح", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "خطأ في فتح المتصفح: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * فتح Google Maps في المتصفح مع إحداثيات
     */
    private static void openGoogleMapsInBrowser(Context context, double latitude, double longitude, String locationName) {
        try {
            String webUri = String.format(Locale.US, 
                "https://www.google.com/maps/search/?api=1&query=%f,%f",
                latitude, longitude);
            
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            if (webIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(webIntent);
                Toast.makeText(context, "فتح الموقع في المتصفح", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "لا يمكن فتح الخرائط. تأكد من تثبيت متصفح", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "خطأ في فتح المتصفح: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * فتح Google Maps في المتصفح باستخدام اسم العميل (دالة مساعدة)
     */
    private static void openGoogleMapsInBrowser(Context context, String customerName) {
        // هذه دالة مساعدة فقط لعرض رسالة
        Toast.makeText(context, "فتح المسار في المتصفح إلى " + customerName, Toast.LENGTH_SHORT).show();
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
}