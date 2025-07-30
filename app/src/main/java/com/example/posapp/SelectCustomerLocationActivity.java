package com.example.posapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
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
    private Button btnMyLocation;
    private Button btnSaveLocation;

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

        // تهيئة مدير الموقع
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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
            selectedMarker.setTitle("موقع العميل");
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
                    selectedMarker.setTitle("موقع العميل");
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

        // إعداد الأزرار
        setupButtons(viewOnly);
    }

    private void setupButtons(boolean viewOnly) {
        btnMyLocation = findViewById(R.id.btn_my_location);
        btnSaveLocation = findViewById(R.id.btn_save_location);

        if (viewOnly) {
            btnSaveLocation.setVisibility(View.GONE);
            btnMyLocation.setVisibility(View.GONE);
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
                    Toast.makeText(SelectCustomerLocationActivity.this, "يرجى تحديد الموقع على الخريطة", Toast.LENGTH_SHORT).show();
                }
            });
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

        // محاولة الحصول على الموقع
        try {
            // التحقق من توفر خدمة GPS
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && 
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(this, getString(R.string.location_service_disabled), Toast.LENGTH_LONG).show();
                return;
            }

            // محاولة الحصول على آخر موقع معروف أولاً
            Location lastKnownLocation = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null) {
                updateLocationOnMap(lastKnownLocation);
            } else {
                // طلب موقع جديد
                Toast.makeText(this, getString(R.string.getting_location), Toast.LENGTH_SHORT).show();
                
                String provider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? 
                    LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
                
                locationManager.requestSingleUpdate(provider, this, null);
            }

        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_SHORT).show();
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
        selectedMarker.setTitle("موقعي الحالي");
        mapView.getOverlays().add(selectedMarker);
        
        // تحديث الموقع المحدد
        selectedGeoPoint = currentLocation;
        
        // تحريك الخريطة للموقع الحالي
        IMapController mapController = mapView.getController();
        mapController.setCenter(currentLocation);
        mapController.setZoom(16.0);
        
        mapView.invalidate();
        Toast.makeText(this, getString(R.string.current_location_set), Toast.LENGTH_SHORT).show();
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
        updateLocationOnMap(location);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // إيقاف طلبات الموقع عند إغلاق النشاط
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                // تجاهل الخطأ
            }
        }
    }
}