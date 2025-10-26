package com.example.islamicquiz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class SelectCustomerLocationActivity extends AppCompatActivity implements LocationListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private MapView mapView;
    private Marker selectedMarker;
    private GeoPoint selectedGeoPoint;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Button btnMyLocation;
    private Button btnSaveLocation;
    private Button btnApplyCoordinates;
    private EditText latInput;
    private EditText lngInput;
    private Handler locationHandler;
    private Runnable locationTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تهيئة osmdroid
        Configuration.getInstance().load(getApplicationContext(), 
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        
        setContentView(R.layout.activity_select_customer_location);

        boolean viewOnly = getIntent().getBooleanExtra("view_only", false);
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);

        // تهيئة مدير الموقع والـ Handler
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationHandler = new Handler();
        
        // تهيئة LocationCallback للموقع المحسن
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // استخدام أول موقع مناسب
                    handleNewLocation(location);
                    break;
                }
            }
        };

        // إعداد الخريطة
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        IMapController mapController = mapView.getController();
        GeoPoint defaultGeoPoint = (lat != 0 && lng != 0) ? new GeoPoint(lat, lng) : new GeoPoint(36.7525, 3.0420);
        mapController.setCenter(defaultGeoPoint);
        mapController.setZoom(14.0);

        // إذا كان هناك موقع محدد مسبقاً، أضف ماركر
        if (lat != 0 && lng != 0) {
            selectedMarker = new Marker(mapView);
            selectedMarker.setPosition(defaultGeoPoint);
            selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            selectedMarker.setTitle(getString(R.string.map_customer_location_title));
            mapView.getOverlays().add(selectedMarker);
            selectedGeoPoint = defaultGeoPoint;
        }

        // إضافة مستمع للنقر على الخريطة (إذا لم يكن في وضع العرض فقط)
        if (!viewOnly) {
            MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    // إزالة الماركر السابق إن وجد
                    if (selectedMarker != null) {
                        mapView.getOverlays().remove(selectedMarker);
                    }
                    
                    // إضافة ماركر جديد
                    selectedMarker = new Marker(mapView);
                    selectedMarker.setPosition(p);
                    selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    selectedMarker.setTitle(getString(R.string.map_customer_location_title));
                    mapView.getOverlays().add(selectedMarker);
                    mapView.invalidate();
                    
                    selectedGeoPoint = p;
                    return true;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };
            
            MapEventsOverlay eventsOverlay = new MapEventsOverlay(mapEventsReceiver);
            mapView.getOverlays().add(0, eventsOverlay);
        }

        // إعداد الأزرار والحقل اليدوي
        setupButtons(viewOnly);
    }

    private void setupButtons(boolean viewOnly) {
        btnMyLocation = findViewById(R.id.btn_my_location);
        btnSaveLocation = findViewById(R.id.btn_save_location);
        btnApplyCoordinates = findViewById(R.id.btn_apply_coordinates);
        latInput = findViewById(R.id.lat_input);
        lngInput = findViewById(R.id.lng_input);

        if (viewOnly) {
            btnSaveLocation.setVisibility(View.GONE);
            btnMyLocation.setVisibility(View.GONE);
            if (btnApplyCoordinates != null) btnApplyCoordinates.setVisibility(View.GONE);
        } else {
            // زر الموقع الحالي
            btnMyLocation.setOnClickListener(v -> getCurrentLocation());

            // زر حفظ الموقع
            btnSaveLocation.setOnClickListener(v -> {
                if (selectedGeoPoint != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("latitude", selectedGeoPoint.getLatitude());
                    resultIntent.putExtra("longitude", selectedGeoPoint.getLongitude());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(SelectCustomerLocationActivity.this, getString(R.string.please_select_location_on_map), Toast.LENGTH_SHORT).show();
                }
            });

            if (btnApplyCoordinates != null) {
                btnApplyCoordinates.setOnClickListener(v -> applyManualCoordinates());
            }
        }
    }

    private void applyManualCoordinates() {
        if (latInput == null || lngInput == null) return;
        String latStr = latInput.getText().toString().trim();
        String lngStr = lngInput.getText().toString().trim();
        if (latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال الإحداثيات أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                Toast.makeText(this, "إحداثيات غير صالحة", Toast.LENGTH_SHORT).show();
                return;
            }
            GeoPoint point = new GeoPoint(lat, lng);
            // إزالة الماركر السابق إن وجد
            if (selectedMarker != null) {
                mapView.getOverlays().remove(selectedMarker);
            }
            selectedMarker = new Marker(mapView);
            selectedMarker.setPosition(point);
            selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            selectedMarker.setTitle(getString(R.string.map_customer_location_title));
            mapView.getOverlays().add(selectedMarker);
            selectedGeoPoint = point;

            // تحريك الخريطة
            IMapController controller = mapView.getController();
            controller.setCenter(point);
            controller.setZoom(18.0);
            mapView.invalidate();

            Toast.makeText(this, "تم تعيين الإحداثيات على الخريطة", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "صيغة أرقام غير صحيحة", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        // التحقق من الصلاحيات
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED && 
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            // طلب الصلاحيات
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // تعطيل الزر مؤقتاً لمنع الضغط المتكرر
        btnMyLocation.setEnabled(false);
                    btnMyLocation.setText(getString(R.string.getting_location_text));

        // أولاً: الحصول على آخر موقع معروف فوراً (سريع جداً)
        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && isLocationFresh(location)) {
                        // عرض آخر موقع معروف فوراً إذا كان حديث
                        updateLocationOnMap(location);
                        Toast.makeText(this, getString(R.string.found_recent_location_improving_accuracy), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.determining_current_location), Toast.LENGTH_SHORT).show();
                    }
                    
                    // بدء طلب موقع محدث للحصول على أعلى دقة
                    requestNewLocation();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.determining_current_location), Toast.LENGTH_SHORT).show();
                    requestNewLocation();
                });
        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.location_services_access_error), Toast.LENGTH_SHORT).show();
            resetLocationButton();
        }
    }
    
    // دالة للتحقق من أن الموقع حديث (أقل من 5 دقائق)
    private boolean isLocationFresh(Location location) {
        return System.currentTimeMillis() - location.getTime() < 5 * 60 * 1000;
    }
    
    // طلب موقع جديد ودقيق باستخدام FusedLocationProviderClient
    private void requestNewLocation() {
        try {
            // إنشاء طلب موقع محسن
            LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)  // أعلى دقة
                .setInterval(2000)  // كل ثانيتين
                .setFastestInterval(1000)  // أسرع تحديث كل ثانية
                .setMaxWaitTime(10000)  // انتظار أقصى 10 ثوان
                .setNumUpdates(5);  // أقصى 5 تحديثات
            
            // بدء طلب الموقع
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            
            // تقليل timeout إلى 12 ثانية للحصول على استجابة أسرع
            locationTimeout = new Runnable() {
                @Override
                public void run() {
                    stopLocationUpdates();
                    resetLocationButton();
                    Toast.makeText(SelectCustomerLocationActivity.this,
                        getString(R.string.location_search_timeout),
                        Toast.LENGTH_LONG).show();
                }
            };
            locationHandler.postDelayed(locationTimeout, 12000); // 12 ثانية

        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.location_services_access_error), Toast.LENGTH_SHORT).show();
            resetLocationButton();
        }
    }

    // دالة للحصول على آخر موقع معروف من جميع المصادر المتاحة
    private Location getLastKnownLocation() {
        try {
            Location bestLocation = null;
            
            // محاولة الحصول على آخر موقع من GPS
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (gpsLocation != null) {
                    bestLocation = gpsLocation;
                }
            }
            
            // محاولة الحصول على آخر موقع من الشبكة
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null) {
                    // اختر الموقع الأحدث أو الأكثر دقة
                    if (bestLocation == null || 
                        networkLocation.getTime() > bestLocation.getTime() ||
                        networkLocation.getAccuracy() < bestLocation.getAccuracy()) {
                        bestLocation = networkLocation;
                    }
                }
            }
            
            // تحقق من أن الموقع ليس قديماً جداً (أكثر من 5 دقائق)
            if (bestLocation != null && 
                System.currentTimeMillis() - bestLocation.getTime() > 5 * 60 * 1000) {
                return null; // الموقع قديم جداً
            }
            
            return bestLocation;
        } catch (SecurityException e) {
            return null;
        }
    }

    private void updateLocationOnMap(Location location) {
        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        
        // إزالة الماركر السابق
        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
        }
        
        // إضافة ماركر للموقع الحالي
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(currentLocation);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // عرض معلومات الدقة
        float accuracy = location.getAccuracy();
        selectedMarker.setTitle(getString(R.string.current_location_title_with_accuracy, Math.round(accuracy)));
        mapView.getOverlays().add(selectedMarker);
        
        // تحديث الموقع المحدد
        selectedGeoPoint = currentLocation;
        
        // تحريك الخريطة للموقع الحالي
        IMapController mapController = mapView.getController();
        mapController.setCenter(currentLocation);
        mapController.setZoom(18.0); // زوم أكبر للدقة
        
        mapView.invalidate();
        
        // إيقاف تحديثات الموقع وإعادة تفعيل الزر
        stopLocationUpdates();
        resetLocationButton();
        
        Toast.makeText(this, getString(R.string.current_location_found_accuracy_meters, Math.round(accuracy)), Toast.LENGTH_SHORT).show();
    }
    
    // دالة لمعالجة الموقع الجديد من FusedLocationProviderClient
    private void handleNewLocation(Location location) {
        float accuracy = location.getAccuracy();
        
        // قبول الموقع إذا كانت الدقة جيدة أو مضى وقت كافي
        if (accuracy <= 50.0f) {
            updateLocationOnMap(location);
            
            // إظهار رسالة نجاح مع معلومات الدقة
            String accuracyText;
            if (accuracy <= 10) {
                accuracyText = getString(R.string.accuracy_excellent);
            } else if (accuracy <= 20) {
                accuracyText = getString(R.string.accuracy_very_good);
            } else if (accuracy <= 50) {
                accuracyText = getString(R.string.accuracy_good);
            } else {
                accuracyText = getString(R.string.accuracy_acceptable);
            }
            
            Toast.makeText(this, getString(R.string.current_location_found_accuracy_with_text, Math.round(accuracy), accuracyText), Toast.LENGTH_SHORT).show();
                
            // إيقاف طلبات الموقع لتوفير البطارية
            stopLocationUpdates();
            resetLocationButton();
        } else {
            // الانتظار للحصول على دقة أفضل
            Toast.makeText(this, getString(R.string.improving_location_accuracy_current, Math.round(accuracy)), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        try {
            // إيقاف FusedLocationProviderClient
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
            
            // إيقاف LocationManager (للتوافق مع الكود القديم)
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        } catch (SecurityException e) {
            // لا حاجة لفعل أي شيء
        }
        
        // إلغاء timeout إذا كان موجوداً
        if (locationHandler != null && locationTimeout != null) {
            locationHandler.removeCallbacks(locationTimeout);
        }
    }
    
    private void resetLocationButton() {
        btnMyLocation.setEnabled(true);
                    btnMyLocation.setText(getString(R.string.my_current_location));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // التحقق من دقة الموقع قبل قبوله
        float accuracy = location.getAccuracy();
        
        // قبول الموقع إذا:
        // 1. دقته أقل من 30 متر (دقة ممتازة)
        // 2. أو دقته أقل من 100 متر ومضى أكثر من 5 ثوان
        // 3. أو مضى أكثر من 10 ثوان (قبول أي دقة)
        long timeSinceStart = System.currentTimeMillis() - location.getTime();
        
        if (accuracy <= 30.0f || 
            (accuracy <= 100.0f && timeSinceStart > 5000) || 
            timeSinceStart > 10000) {
            
            updateLocationOnMap(location);
            
            // إظهار رسالة نجاح مع معلومات الدقة
            String accuracyText;
            if (accuracy <= 10) {
                accuracyText = "ممتازة";
            } else if (accuracy <= 30) {
                accuracyText = "جيدة جداً";
            } else if (accuracy <= 100) {
                accuracyText = "جيدة";
            } else {
                accuracyText = "مقبولة";
            }
            
            Toast.makeText(this, "✅ تم تحديد موقعك الحالي - الدقة: " + 
                Math.round(accuracy) + "م (" + accuracyText + ")", Toast.LENGTH_SHORT).show();
        } else {
            // الانتظار للحصول على دقة أفضل مع إظهار التقدم
            Toast.makeText(this, "🔄 جاري تحسين دقة الموقع... (حالياً: " + 
                Math.round(accuracy) + "م)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Toast.makeText(this, getString(R.string.provider_enabled, provider), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, getString(R.string.provider_disabled, provider), Toast.LENGTH_SHORT).show();
        resetLocationButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        // إيقاف تحديثات الموقع عند إيقاف النشاط مؤقتاً
        stopLocationUpdates();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // التأكد من إيقاف جميع تحديثات الموقع والـ Handler
        stopLocationUpdates();
        if (locationHandler != null) {
            locationHandler.removeCallbacksAndMessages(null);
        }
        
        // تنظيف FusedLocationProviderClient
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (SecurityException e) {
                // تجاهل الأخطاء عند التنظيف
            }
        }
    }


}